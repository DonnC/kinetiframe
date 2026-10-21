"""Offline source contract checks; visual acceptance uses the Android compositor."""
import math
import unittest
from cast_views import cast_actor, VIEWS
from street_actor import world_pose


class DirectionalBindingsTest(unittest.TestCase):
    def test_directional_hand_swaps_register_wrist_and_preserve_previous_revision(self):
        for character in ('tavi', 'mira', 'kuda'):
            for view in VIEWS:
                old, old_art = cast_actor(character, view, directional=True)
                revision, artwork = cast_actor(character, view, gestures=True)
                self.assertNotEqual(old['revisionId'], revision['revisionId'])
                for side in ('left', 'right'):
                    bone = revision['sceneGraph']['bones'][f'{side}_hand']
                    def wrist_width(image, fraction):
                        xs = [x for x in range(image.width) if image.getpixel((x, round(image.height*fraction)))[3] > 150]
                        return xs[-1]-xs[0]+1
                    rest_width = wrist_width(artwork[f'{side}_hand'], .05)*bone['textureBinding']['scale']
                    for variant in ('palm', 'point', 'fist'):
                        address = f'{side}_hand#{variant}'
                        image = artwork[address]
                        binding = bone['artworkVariants'][variant]['binding']
                        self.assertNotEqual(old_art[address].tobytes(), image.tobytes())
                        self.assertTrue(0 < binding['pivotX'] < image.width)
                        self.assertAlmostEqual(.95, binding['pivotY']/image.height)
                        self.assertEqual(180, binding['rotationDegrees'])
                        self.assertTrue(math.isfinite(binding['scale']) and binding['scale'] > 0)
                        self.assertAlmostEqual(rest_width, wrist_width(image, .95)*binding['scale'])

    def test_profile_stationary_contact_does_not_reintroduce_front_foot_spread(self):
        from street_motion import pose
        for character in ('tavi', 'mira', 'kuda'):
            revision, _ = cast_actor(character, 'PROFILE', directional=True)
            key, _ = pose(revision, 0, 'mira')
            left, right = key['contacts']['left_shin'], key['contacts']['right_shin']
            self.assertAlmostEqual(left['x'], right['x'])
            self.assertAlmostEqual(left['y'], right['y'])

    def test_every_view_has_distinct_authored_feet_and_complete_sources(self):
        for character in ('tavi', 'mira', 'kuda'):
            footprints = []
            for view in VIEWS:
                revision, artwork = cast_actor(character, view, directional=True)
                self.assertIn('all-views-v2', revision['revisionId'])
                self.assertEqual(set(artwork), set(revision['artworkAssetPaths']))
                footprints.append(artwork['right_foot'].tobytes())
                if view == 'BACK': self.assertNotIn('eyes', revision['sceneGraph']['bones'])
                for image in artwork.values():
                    self.assertEqual('RGBA', image.mode)
                    self.assertIsNotNone(image.getbbox())
            self.assertEqual(4, len(set(footprints)))

    def test_profile_hidden_limbs_share_rest_sockets_but_remain_posable(self):
        for character in ('tavi', 'mira', 'kuda'):
            revision, _ = cast_actor(character, 'PROFILE', directional=True)
            graph = revision['sceneGraph']
            rotations = {bid: b['localTransform']['rotationDegrees'] for bid, b in graph['bones'].items()}
            rest = world_pose(graph, rotations, {})
            for part in ('upper_arm', 'thigh', 'foot', 'hand'):
                self.assertEqual(rest[f'left_{part}'], rest[f'right_{part}'])
            rotations['left_thigh'] += 30
            posed = world_pose(graph, rotations, {})
            self.assertNotEqual(posed['left_foot'][0], posed['right_foot'][0])

    def test_rebound_meshes_are_finite_and_match_the_current_rest_skeleton(self):
        for character in ('tavi', 'mira', 'kuda'):
            for view in VIEWS:
                revision, _ = cast_actor(character, view, directional=True)
                graph = revision['sceneGraph']
                rest = world_pose(graph, {bid:b['localTransform']['rotationDegrees'] for bid,b in graph['bones'].items()}, {})
                skins = [b['textureBinding']['skin'] for b in graph['bones'].values() if 'skin' in b['textureBinding']]
                self.assertEqual(4, len(skins))
                for skin in skins:
                    for influence in skin['bones']:
                        origin = rest[influence['boneId']][0]
                        self.assertAlmostEqual(origin[0], influence['bindWorld']['tx'])
                        self.assertAlmostEqual(origin[1], influence['bindWorld']['ty'])
                    for vertex in skin['vertices']:
                        self.assertTrue(math.isfinite(vertex['x']) and math.isfinite(vertex['y']))
                        self.assertAlmostEqual(1, sum(vertex['weights']))


if __name__ == '__main__': unittest.main()
