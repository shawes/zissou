#!/usr/bin/env python3
"""Converts ROMS output to the monthly daily-mean flow files Zissou reads, so that a regional
ROMS model can be a nested flow (see "Finer models nested inside the flow" in the README).

    scripts/roms_to_zissou.py --out /data/roms-eac --resolution 0.02 roms_his_*.nc

Needs numpy, netCDF4 and scipy:  pip install numpy netCDF4 scipy

What it does, for each record of the ROMS files:

  1. brings u and v from the staggered C-grid to the cell centres (rho points), without letting
     land pull the flow towards zero at the coast;
  2. rotates them from the grid's axes to east and north, with the grid's `angle`;
  3. interpolates u, v and w from the terrain-following levels to fixed depths, using the depth
     of each level from s_rho, Cs_r, hc, h and zeta (Vtransform 1 or 2). Depths below the sea
     floor have no flow;
  4. interpolates from the curvilinear ROMS grid to a regular longitude/latitude grid. A node is
     land where the nearest ROMS cell is land, so the coast is ROMS's own;
  5. averages the records of each UTC day.

and writes <out>/{u,v,w}/<prefix>_<variable>_<year>_<month>.nc, a field for every day of the month,
stamped at noon, with u, v and w on one grid and the depths as `st_ocean`. A month the ROMS files
do not cover in full is not written (Zissou reads a whole month), and the script says which.

ROMS's `w` is the vertical velocity in m/s. If the files have none, w is written as 0 where there
is flow, and flow.includeVerticalVelocity should be false.

The times of the ROMS output are taken as UTC, as Zissou takes its clock.
"""
import argparse
import datetime as dt
import os
import sys

import numpy as np
import netCDF4
from scipy.spatial import cKDTree

# BRAN2020's first 15 levels, which is also Zissou's default number of depth levels
DEFAULT_DEPTHS = [2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0]
FILL = np.float32(-1.0e20)
TIME_NAMES = ("ocean_time", "time", "Time", "ocean_time_0")


def warn(message):
    print("warning: " + message, file=sys.stderr)


def read(variable, default=None):
    """A variable's values as floats with NaN for what is masked or fill, or `default` if it is missing."""
    if variable is None:
        return default
    values = np.ma.filled(np.ma.masked_invalid(variable[:]).astype(np.float64), np.nan)
    values[np.abs(values) > 1.0e10] = np.nan
    return values


def find(nc, *names):
    for name in names:
        if name in nc.variables:
            return nc.variables[name]
    return None


# ---- vertical ------------------------------------------------------------------------------

def level_depths(vtransform, s, cs, hc, h, zeta):
    """The height z (negative down, m) of each terrain-following level at each cell: shape (levels, eta, xi)."""
    s = s[:, None, None]
    cs = cs[:, None, None]
    if vtransform == 1:
        z0 = hc * s + (h - hc) * cs
        return z0 + zeta * (1.0 + z0 / h)
    z0 = (hc * s + h * cs) / (hc + h)
    return zeta + (zeta + h) * z0


def to_depths(field, z, depths, h):
    """Interpolates a field on levels at heights z to fixed depths (m, positive down), linearly between
    levels and held at the end levels beyond them; NaN below the sea floor. Shape (len(depths), eta, xi)."""
    n = field.shape[0]
    out = np.empty((len(depths),) + field.shape[1:], dtype=np.float32)
    for i, depth in enumerate(depths):
        target = -depth
        k = np.clip((z <= target).sum(axis=0) - 1, 0, n - 2)[None]
        z0 = np.take_along_axis(z, k, 0)[0]
        z1 = np.take_along_axis(z, k + 1, 0)[0]
        f0 = np.take_along_axis(field, k, 0)[0]
        f1 = np.take_along_axis(field, k + 1, 0)[0]
        span = z1 - z0
        fraction = np.clip((target - z0) / np.where(span > 0, span, 1.0), 0.0, 1.0)
        value = f0 + (f1 - f0) * fraction
        out[i] = np.where(target < -h, np.nan, value)
    return out


# ---- horizontal ----------------------------------------------------------------------------

def to_centres(field, axis, mask_centres):
    """Averages a C-grid u (axis=-1) or v (axis=-2) field, of one fewer cell along that axis, onto the cell
    centres, from whichever of the two faces have flow, so that land does not pull it towards zero."""
    pad = [(0, 0)] * field.ndim
    pad[axis] = (1, 0)
    before = np.pad(field, pad, constant_values=np.nan)
    pad[axis] = (0, 1)
    after = np.pad(field, pad, constant_values=np.nan)
    count = np.isfinite(before).astype(float) + np.isfinite(after)
    total = np.nan_to_num(before) + np.nan_to_num(after)
    centres = np.where(count > 0, total / np.where(count > 0, count, 1.0), np.nan)
    return np.where(mask_centres, centres, np.nan)


class Regridder:
    """Interpolates fields at the cell centres of a curvilinear grid to a regular longitude/latitude grid:
    inverse-distance from the 4 nearest cells that have data. A node is land (NaN) where the nearest cell is
    land, or has no data at that depth, and outside the grid where it is more than 1.5 cells from the nearest."""

    def __init__(self, lon, lat, mask, lons, lats, neighbours=4):
        scale = np.cos(np.radians(np.nanmean(lat)))
        cells = np.column_stack([lon.ravel() * scale, lat.ravel()])
        tree = cKDTree(cells)
        spacing = tree.query(cells, k=2)[0][:, 1]
        grid_lon, grid_lat = np.meshgrid(lons, lats)
        nodes = np.column_stack([grid_lon.ravel() * scale, grid_lat.ravel()])
        distance, self.index = tree.query(nodes, k=neighbours)
        sea = mask.ravel().astype(bool)
        nearest = self.index[:, 0]
        self.inside = (distance[:, 0] <= 1.5 * spacing[nearest]) & sea[nearest]
        self.weight = np.where(sea[self.index], 1.0 / np.maximum(distance, 1.0e-9), 0.0)
        self.shape = grid_lon.shape

    def __call__(self, field):
        depths = field.shape[0]
        values = field.reshape(depths, -1)[:, self.index]
        valid = np.isfinite(values) & (self.weight > 0)
        weights = np.where(valid, self.weight, 0.0)
        total = weights.sum(axis=-1)
        mean = (np.where(valid, values, 0.0) * weights).sum(axis=-1) / np.where(total > 0, total, 1.0)
        mean = np.where(valid[..., 0] & self.inside, mean, np.nan)
        return mean.reshape((depths,) + self.shape).astype(np.float32)


# ---- output --------------------------------------------------------------------------------

class Month:
    """The three files of one month, filled a day at a time; deleted if the month is not filled."""

    def __init__(self, out, prefix, year, month, lons, lats, depths):
        self.year, self.month = year, month
        self.days = (dt.date(year + month // 12, month % 12 + 1, 1) - dt.date(year, month, 1)).days
        self.written = set()
        self.files = {}
        for variable in ("u", "v", "w"):
            folder = os.path.join(out, variable)
            os.makedirs(folder, exist_ok=True)
            path = os.path.join(folder, "%s_%s_%d_%02d.nc" % (prefix, variable, year, month))
            nc = netCDF4.Dataset(path, "w", format="NETCDF3_64BIT_OFFSET")
            nc.createDimension("Time", self.days)
            nc.createDimension("st_ocean", len(depths))
            nc.createDimension("yu_ocean", len(lats))
            nc.createDimension("xu_ocean", len(lons))
            t = nc.createVariable("Time", "f8", ("Time",))
            t.units, t.calendar, t.axis = "days since %d-%02d-01 00:00:00" % (year, month), "gregorian", "T"
            t[:] = np.arange(self.days) + 0.5
            z = nc.createVariable("st_ocean", "f8", ("st_ocean",))
            z.units, z.positive, z.axis = "m", "down", "Z"
            z[:] = depths
            y = nc.createVariable("yu_ocean", "f8", ("yu_ocean",))
            y.units, y.axis = "degrees_north", "Y"
            y[:] = lats
            x = nc.createVariable("xu_ocean", "f8", ("xu_ocean",))
            x.units, x.axis = "degrees_east", "X"
            x[:] = lons
            data = nc.createVariable(variable, "f4", ("Time", "st_ocean", "yu_ocean", "xu_ocean"), fill_value=FILL)
            data.units, data.missing_value = "m/s", FILL
            self.files[variable] = (nc, path)

    def add(self, day, fields):
        for variable, (nc, _) in self.files.items():
            nc.variables[variable][day - 1] = np.ma.masked_invalid(fields[variable])
        self.written.add(day)

    def close(self):
        complete = len(self.written) == self.days
        for nc, path in self.files.values():
            nc.close()
            if not complete:
                os.remove(path)
        if not complete:
            warn("%d-%02d has %d of its %d days, so it was not written" % (self.year, self.month, len(self.written), self.days))
        return complete


# ---- conversion ----------------------------------------------------------------------------

class Grid:
    """The ROMS grid, read once, with what is needed to turn its records into fields on fixed depths."""

    def __init__(self, nc, depths, lons, lats, vtransform=None):
        self.lon = read(find(nc, "lon_rho"))
        self.lat = read(find(nc, "lat_rho"))
        if self.lon is None or self.lat is None:
            sys.exit("error: the grid has no lon_rho and lat_rho; give the file that has them with --grid")
        self.mask = read(find(nc, "mask_rho"), np.ones(self.lon.shape)) > 0.5
        mask_u = read(find(nc, "mask_u"))
        mask_v = read(find(nc, "mask_v"))
        self.mask_u = (mask_u > 0.5) if mask_u is not None else self.mask[:, :-1] & self.mask[:, 1:]
        self.mask_v = (mask_v > 0.5) if mask_v is not None else self.mask[:-1, :] & self.mask[1:, :]
        angle = find(nc, "angle")
        if angle is None:
            warn("the grid has no angle, so u and v are taken to be east and north already")
        self.angle = read(angle, np.zeros(self.lon.shape))
        self.h = np.where(self.mask, np.nan_to_num(read(find(nc, "h"), np.full(self.lon.shape, np.nan)), nan=1.0), 1.0)
        if np.all(self.h == 1.0):
            sys.exit("error: the grid has no depth h, so the depth of the levels cannot be found")
        self.h = np.maximum(self.h, 1.0e-3)
        self.s_rho, self.cs_r = read(find(nc, "s_rho")), read(find(nc, "Cs_r"))
        self.s_w, self.cs_w = read(find(nc, "s_w")), read(find(nc, "Cs_w"))
        if self.s_rho is None or self.cs_r is None:
            sys.exit("error: the files have no s_rho and Cs_r, so the depth of the levels cannot be found")
        hc = find(nc, "hc")
        self.hc = float(hc[:]) if hc is not None else None
        if self.hc is None:
            sys.exit("error: the files have no hc (the critical depth of the stretching)")
        if vtransform is None:
            found = find(nc, "Vtransform")
            vtransform = int(found[:]) if found is not None else 1
            if found is None:
                warn("the files have no Vtransform, so 1 is assumed; give --vtransform if it is 2")
        self.vtransform = vtransform
        self.depths = depths
        self.regrid = Regridder(self.lon, self.lat, self.mask, lons, lats)

    def convert(self, nc, t):
        """The east, north and up flow at fixed depths on the output grid, for a record."""
        zeta_var = find(nc, "zeta")
        zeta = np.nan_to_num(read(zeta_var[t] if zeta_var is not None else None, np.zeros(self.lon.shape)))
        zeta = np.where(self.mask, zeta, 0.0)
        z_rho = level_depths(self.vtransform, self.s_rho, self.cs_r, self.hc, self.h, zeta)
        u = read(nc.variables["u"][t])
        v = read(nc.variables["v"][t])
        u = np.where(self.mask_u, u, np.nan)
        v = np.where(self.mask_v, v, np.nan)
        u = to_centres(u, -1, self.mask)
        v = to_centres(v, -2, self.mask)
        cos, sin = np.cos(self.angle), np.sin(self.angle)
        east, north = u * cos - v * sin, u * sin + v * cos
        out = {
            "u": self.regrid(to_depths(east, z_rho, self.depths, self.h)),
            "v": self.regrid(to_depths(north, z_rho, self.depths, self.h)),
        }
        w = nc.variables.get("w")
        if w is not None and self.s_w is not None and self.cs_w is not None:
            z_w = level_depths(self.vtransform, self.s_w, self.cs_w, self.hc, self.h, zeta)
            up = np.where(self.mask, read(w[t]), np.nan)
            out["w"] = self.regrid(to_depths(up, z_w, self.depths, self.h))
        else:
            out["w"] = np.where(np.isfinite(out["u"]), np.float32(0.0), np.float32(np.nan))
        return out


def days_of(nc):
    """The UTC (year, month, day) of each record."""
    variable = find(nc, *TIME_NAMES)
    if variable is None:
        sys.exit("error: the file has none of the time variables %s" % ", ".join(TIME_NAMES))
    dates = netCDF4.num2date(variable[:], variable.units, getattr(variable, "calendar", "standard"),
                             only_use_cftime_datetimes=True)
    return [(d.year, d.month, d.day) for d in np.atleast_1d(dates)]


def first_day(path):
    with netCDF4.Dataset(path) as nc:
        return days_of(nc)[0]


def convert(arguments):
    files = sorted(arguments.files, key=first_day)
    with netCDF4.Dataset(arguments.grid or files[0]) as nc:
        lon, lat = read(find(nc, "lon_rho")), read(find(nc, "lat_rho"))
    if lon is None:
        sys.exit("error: %s has no lon_rho and lat_rho; give the file that has them with --grid" % (arguments.grid or files[0]))
    west = lon.min() if arguments.west is None else arguments.west
    east = lon.max() if arguments.east is None else arguments.east
    south = lat.min() if arguments.south is None else arguments.south
    north = lat.max() if arguments.north is None else arguments.north
    step = arguments.resolution
    lons = np.round(np.arange(west, east + step / 2, step), 6)
    lats = np.round(np.arange(south, north + step / 2, step), 6)
    depths = [float(d) for d in arguments.depths.split(",")] if arguments.depths else DEFAULT_DEPTHS
    print("Output grid: %d x %d nodes of %g degrees, %g to %g east, %g to %g north, %d depths"
          % (len(lons), len(lats), step, lons[0], lons[-1], lats[0], lats[-1], len(depths)))

    with netCDF4.Dataset(arguments.grid or files[0]) as nc:
        grid = Grid(nc, depths, lons, lats, arguments.vtransform)

    shape = (len(depths), len(lats), len(lons))
    total = {v: np.zeros(shape) for v in "uvw"}
    count = {v: np.zeros(shape) for v in "uvw"}
    current = None  # the day being averaged
    month = None  # the Month being written
    written = []
    last_day = None

    def flush():
        nonlocal month
        if current is None:
            return
        year, mon, day = current
        if month is None or (month.year, month.month) != (year, mon):
            if month is not None and month.close():
                written.append((month.year, month.month))
            month = Month(arguments.out, arguments.prefix, year, mon, lons, lats, depths)
        fields = {v: np.where(count[v] > 0, total[v] / np.where(count[v] > 0, count[v], 1.0), np.nan) for v in "uvw"}
        month.add(day, fields)
        for v in "uvw":
            total[v][:] = 0.0
            count[v][:] = 0.0

    for path in files:
        print("Reading %s" % path)
        with netCDF4.Dataset(path) as nc:
            for t, day in enumerate(days_of(nc)):
                if last_day is not None and day < last_day:
                    sys.exit("error: the records are not in time order at %s record %d" % (path, t))
                last_day = day
                if day != current:
                    flush()
                    current = day
                fields = grid.convert(nc, t)
                for v in "uvw":
                    finite = np.isfinite(fields[v])
                    total[v] += np.where(finite, fields[v], 0.0)
                    count[v] += finite
    flush()
    if month is not None and month.close():
        written.append((month.year, month.month))

    if not written:
        sys.exit("error: no month was covered in full, so nothing was written")
    print("Wrote %d months, %d-%02d to %d-%02d, under %s"
          % (len(written), written[0][0], written[0][1], written[-1][0], written[-1][1], arguments.out))
    print("\nFor the configuration, under flow:\n  nested:\n    - netcdfFilePath: %s\n"
          "      bounds: {south: %g, north: %g, west: %g, east: %g}\n      depthLevels: %d\n      filePrefix: %s"
          % (os.path.abspath(arguments.out), lats[0], lats[-1], lons[0], lons[-1], len(depths), arguments.prefix))
    if find_w_missing(files[0]):
        print("\nThe files have no w: set flow.includeVerticalVelocity to false.")


def find_w_missing(path):
    with netCDF4.Dataset(path) as nc:
        return "w" not in nc.variables


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("files", nargs="+", help="the ROMS history or average files, in any order")
    parser.add_argument("--out", required=True, help="the folder to write u/, v/ and w/ under")
    parser.add_argument("--resolution", type=float, required=True,
                        help="the spacing of the output grid, in degrees; about the ROMS grid's own (0.02 is 2 km)")
    parser.add_argument("--prefix", default="ocean", help="the start of the file names (flow.filePrefix); default ocean")
    parser.add_argument("--south", type=float)
    parser.add_argument("--north", type=float)
    parser.add_argument("--west", type=float)
    parser.add_argument("--east", type=float, help="the part of the ROMS grid to write; the whole of it if not given")
    parser.add_argument("--depths", help="the depths of the levels, m positive down, comma-separated; BRAN2020's top 15 if not given")
    parser.add_argument("--grid", help="a file with lon_rho, lat_rho, mask_rho, angle, h, hc and the stretching, if the "
                                       "output files do not carry them")
    parser.add_argument("--vtransform", type=int, choices=(1, 2), help="the ROMS vertical transform, if the files do not say")
    convert(parser.parse_args())


if __name__ == "__main__":
    main()
