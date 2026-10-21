"""Motion invariants independent of saved demo projects."""
import unittest

from street_actor import actor
from street_motion import foot, performance


class StreetMotionTests(unittest.TestCase):
    def test_stance_anchor_is_stationary_and_swing_lifts_forward(self):
        for side in ('left', 'right'):
            samples = [foot(frame / 240, side) for frame in range(1440)]
            for before, after in zip(samples, samples[1:]):
                if before[2] and after[2]:
                    self.assertAlmostEqual(before[0], after[0], places=7)
            self.assertTrue(any(y < 770 for _, y, planted in samples if not planted))

    def test_walk_moves_forward_and_stop_has_zero_velocity(self):
        revision, _ = actor('tavi', True)
        motion = performance(revision, 'approach', 6, 'motion-test')
        positions = [key['rootTranslations']['torso']['x'] for key in motion['poseKeys']]
        self.assertTrue(all(b >= a for a, b in zip(positions, positions[1:])))
        self.assertLess(positions[-1] - positions[-2], .1)
