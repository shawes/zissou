"""Tests for roms_to_zissou.py, on a small ROMS-like model made up here: a grid rotated 20 degrees,
terrain-following levels over a sloping sea floor, and land along its east side, with the same flow
(0.5 m/s east, 0.1 m/s north, 0.001 m/s up) everywhere in the sea.

    python3 scripts/test_roms_to_zissou.py        (needs numpy, netCDF4 and scipy)
"""
import argparse
import os
import sys
import tempfile
import unittest

import numpy as np
import netCDF4

sys.path.insert(0, os.path.dirname(__file__))
import roms_to_zissou as convert

ANGLE = np.radians(20.0)
EAST, NORTH, UP = 0.5, 0.1, 0.001
NY, NX, NS = 24, 30, 8
CELL = 0.02  # degrees
LAND_FROM = 26  # columns this far east and beyond are land


def make_roms(path, days, year=2002, month=2, vtransform=2, with_w=True):
    lat0, lon0 = -28.0, 153.0
    eta, xi = np.meshgrid(np.arange(NY), np.arange(NX), indexing="ij")
    # a lattice rotated by ANGLE
    lon = lon0 + CELL * (xi * np.cos(ANGLE) - eta * np.sin(ANGLE)) / np.cos(np.radians(lat0))
    lat = lat0 + CELL * (xi * np.sin(ANGLE) + eta * np.cos(ANGLE))
    mask = (xi < LAND_FROM).astype(float)
    h = 40.0 + 10.0 * xi  # 40 m in the west, 330 m in the east
    s_rho = (np.arange(NS) - NS + 0.5) / NS
    s_w = (np.arange(NS + 1) - NS) / NS
    cs = lambda s: (1 - np.cosh(2.0 * s)) / (np.cosh(2.0) - 1)
    with netCDF4.Dataset(path, "w") as nc:
        for name, n in (("ocean_time", None), ("s_rho", NS), ("s_w", NS + 1), ("eta_rho", NY), ("xi_rho", NX),
                        ("eta_u", NY), ("xi_u", NX - 1), ("eta_v", NY - 1), ("xi_v", NX)):
            nc.createDimension(name, n)
        t = nc.createVariable("ocean_time", "f8", ("ocean_time",))
        t.units = "seconds since %d-%02d-01 00:00:00" % (year, month)
        t[:] = (np.arange(days) + 0.5) * 86400.0
        for name, value, dims in (("lon_rho", lon, ("eta_rho", "xi_rho")), ("lat_rho", lat, ("eta_rho", "xi_rho")),
                                  ("mask_rho", mask, ("eta_rho", "xi_rho")), ("h", h, ("eta_rho", "xi_rho")),
                                  ("angle", np.full(h.shape, ANGLE), ("eta_rho", "xi_rho")),
                                  ("s_rho", s_rho, ("s_rho",)), ("Cs_r", cs(s_rho), ("s_rho",)),
                                  ("s_w", s_w, ("s_w",)), ("Cs_w", cs(s_w), ("s_w",))):
            nc.createVariable(name, "f8", dims)[:] = value
        nc.createVariable("hc", "f8", ())[:] = 10.0
        nc.createVariable("Vtransform", "i4", ())[:] = vtransform
        # the flow in the grid's own axes: east and north turned back by the angle
        u_grid = EAST * np.cos(ANGLE) + NORTH * np.sin(ANGLE)
        v_grid = -EAST * np.sin(ANGLE) + NORTH * np.cos(ANGLE)
        mask_u = mask[:, :-1] * mask[:, 1:]
        mask_v = mask[:-1, :] * mask[1:, :]
        nc.createVariable("zeta", "f4", ("ocean_time", "eta_rho", "xi_rho"))[:] = np.zeros((days, NY, NX))
        nc.createVariable("u", "f4", ("ocean_time", "s_rho", "eta_u", "xi_u"))[:] = u_grid * mask_u * np.ones((days, NS, 1, 1))
        nc.createVariable("v", "f4", ("ocean_time", "s_rho", "eta_v", "xi_v"))[:] = v_grid * mask_v * np.ones((days, NS, 1, 1))
        if with_w:
            nc.createVariable("w", "f4", ("ocean_time", "s_w", "eta_rho", "xi_rho"))[:] = UP * mask * np.ones((days, NS + 1, 1, 1))
    return lon, lat


def run(files, out, **options):
    defaults = dict(out=out, resolution=CELL, prefix="ocean", south=None, north=None, west=None, east=None,
                    depths=None, grid=None, vtransform=None, files=files)
    defaults.update(options)
    convert.convert(argparse.Namespace(**defaults))


class RomsToZissouTest(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.roms = os.path.join(self.dir.name, "roms.nc")
        self.out = os.path.join(self.dir.name, "out")

    def tearDown(self):
        self.dir.cleanup()

    def read(self, variable, month="2002_02"):
        nc = netCDF4.Dataset(os.path.join(self.out, variable, "ocean_%s_%s.nc" % (variable, month)))
        data = np.ma.filled(nc.variables[variable][:], np.nan)
        return nc, data

    def test_a_full_month_is_written_with_a_day_for_every_day_and_noon_stamps(self):
        make_roms(self.roms, 28)
        run([self.roms], self.out)
        nc, u = self.read("u")
        self.assertEqual(u.shape[:2], (28, 15))
        self.assertEqual(list(nc.variables["Time"][:3]), [0.5, 1.5, 2.5])
        self.assertEqual(nc.variables["Time"].units, "days since 2002-02-01 00:00:00")
        self.assertEqual(list(nc.variables["st_ocean"][:3]), [2.5, 7.5, 12.5])

    def test_the_flow_is_east_and_north_not_along_the_rotated_grid(self):
        make_roms(self.roms, 28)
        run([self.roms], self.out)
        _, u = self.read("u")
        _, v = self.read("v")
        _, w = self.read("w")
        # the middle of the sea, in the surface level
        y, x = u.shape[2] // 2, u.shape[3] // 3
        self.assertAlmostEqual(u[3, 0, y, x], EAST, places=3)
        self.assertAlmostEqual(v[3, 0, y, x], NORTH, places=3)
        self.assertAlmostEqual(w[3, 0, y, x], UP, places=6)

    def test_there_is_no_flow_on_land_or_below_the_sea_floor(self):
        lon, lat = make_roms(self.roms, 28)
        run([self.roms], self.out)
        nc, u = self.read("u")
        lons, lats = nc.variables["xu_ocean"][:], nc.variables["yu_ocean"][:]
        grid_lon, grid_lat = np.meshgrid(lons, lats)
        # land: a node as far east as the land columns, near the middle row
        # the lattice is rotated: find the nodes by the ROMS cell they are nearest
        cells = np.column_stack([lon.ravel() * np.cos(np.radians(-28.0)), lat.ravel()])
        nodes = np.column_stack([grid_lon.ravel() * np.cos(np.radians(-28.0)), grid_lat.ravel()])
        from scipy.spatial import cKDTree
        distance, nearest = cKDTree(cells).query(nodes)
        xi = (nearest % NX).reshape(grid_lon.shape)
        near = (distance.reshape(grid_lon.shape) < CELL * 0.5)
        on_land = near & (xi >= LAND_FROM)
        in_west = near & (xi <= 3)
        self.assertTrue(on_land.any() and in_west.any())
        self.assertTrue(np.isnan(u[0, 0][on_land]).all(), "land has flow")
        self.assertTrue(np.isfinite(u[0, 0][in_west]).all(), "the sea has none at the surface")
        # the sea floor is 40 m deep in the west: nothing at 48.5 m and below there, though flow at 28 m
        self.assertTrue(np.isfinite(u[0, 5][in_west]).all())  # 28.2 m
        self.assertTrue(np.isnan(u[0, 8][in_west & (xi == 0)]).all())  # 48.5 m, over a floor at 40 m

    def test_the_days_are_averaged(self):
        # hourly records: 24 a day, flow east 0.5 on the first half of each day and 0 on the other
        times = (np.arange(28 * 24) + 0.5) * 3600.0
        hourly = os.path.join(self.dir.name, "hourly.nc")
        make_roms(hourly, 28 * 24)
        with netCDF4.Dataset(hourly, "a") as nc:
            nc.variables["ocean_time"][:] = times
            scale = np.tile(np.r_[np.ones(12), np.zeros(12)], 28)
            nc.variables["u"][:] = nc.variables["u"][:] * scale[:, None, None, None]
            nc.variables["v"][:] = nc.variables["v"][:] * scale[:, None, None, None]
        run([hourly], self.out)
        _, u = self.read("u")
        _, v = self.read("v")
        y, x = u.shape[2] // 2, u.shape[3] // 3
        self.assertAlmostEqual(u[10, 0, y, x], EAST / 2, places=3)
        self.assertAlmostEqual(v[10, 0, y, x], NORTH / 2, places=3)

    def test_a_month_that_is_not_covered_in_full_is_not_written(self):
        make_roms(self.roms, 10)
        with self.assertRaises(SystemExit):
            run([self.roms], self.out)
        self.assertFalse(os.path.exists(os.path.join(self.out, "u", "ocean_u_2002_02.nc")))

    def test_files_in_any_order_make_one_continuous_month(self):
        first, second = os.path.join(self.dir.name, "a.nc"), os.path.join(self.dir.name, "b.nc")
        make_roms(first, 14)
        make_roms(second, 14)
        with netCDF4.Dataset(second, "a") as nc:
            nc.variables["ocean_time"][:] = (np.arange(14) + 14.5) * 86400.0
        run([second, first], self.out)
        _, u = self.read("u")
        self.assertEqual(u.shape[0], 28)
        self.assertTrue(np.isfinite(u[0, 0]).any() and np.isfinite(u[27, 0]).any())

    def test_without_w_it_is_zero_where_there_is_flow(self):
        make_roms(self.roms, 28, with_w=False)
        run([self.roms], self.out)
        _, u = self.read("u")
        _, w = self.read("w")
        self.assertTrue((w[np.isfinite(u)] == 0).all() and np.isnan(w[np.isnan(u)]).all())

    def test_vtransform_1_gives_the_same_flow(self):
        make_roms(self.roms, 28, vtransform=1)
        run([self.roms], self.out)
        _, u = self.read("u")
        y, x = u.shape[2] // 2, u.shape[3] // 3
        self.assertAlmostEqual(u[0, 0, y, x], EAST, places=3)


class LevelsTest(unittest.TestCase):

    def test_levels_run_from_the_floor_to_the_surface_for_both_transforms(self):
        s = np.array([-0.75, -0.25])
        cs = np.array([-0.8, -0.2])
        h = np.full((1, 1), 100.0)
        for vtransform in (1, 2):
            z = convert.level_depths(vtransform, s, cs, 10.0, h, np.zeros((1, 1)))[:, 0, 0]
            self.assertTrue(-100.0 < z[0] < z[1] < 0.0, (vtransform, z))

    def test_interpolating_between_levels_and_holding_beyond_them(self):
        z = np.array([-80.0, -20.0]).reshape(2, 1, 1)
        field = np.array([1.0, 3.0]).reshape(2, 1, 1)
        out = convert.to_depths(field, z, [5.0, 50.0, 90.0, 120.0], np.full((1, 1), 100.0))[:, 0, 0]
        self.assertAlmostEqual(out[0], 3.0)  # above the top level: held
        self.assertAlmostEqual(out[1], 2.0)  # halfway between the levels
        self.assertAlmostEqual(out[2], 1.0)  # between the bottom level and the floor: held
        self.assertTrue(np.isnan(out[3]))  # below the floor


if __name__ == "__main__":
    unittest.main()
