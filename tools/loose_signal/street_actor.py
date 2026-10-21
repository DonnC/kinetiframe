"""Source-pixel rigs. All bindings register to joints; expressions keep a shared face scale."""
import math
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ORIGIN = (960., 600.)
STAMP = 1791417600000


def actor(character, profile=False):
    folder = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v2/cutouts'
    images = {p.stem: Image.open(p).convert('RGBA') for p in folder.glob('*.png')}
    continuous_folder = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v3/cutouts'
    continuous = {p.stem: Image.open(p).convert('RGBA') for p in continuous_folder.glob('*.png')}
    for name in ['torso', 'profile-torso']:
        if name in continuous: images[name] = continuous[name]
    bones, artwork = {}, {}
    def add(bid, parent, x, y, angle, length, source, height, pivot, order, correction=0):
        image = images[source]
        binding = {'assetId': bid, 'pivotX': image.width*pivot[0], 'pivotY': image.height*pivot[1],
                   'scale': height/image.height, 'rotationDegrees': correction}
        bones[bid] = {'id':bid,'name':bid.replace('_',' ').title(),'parentId':parent,'length':length,
            'localTransform':{'x':x,'y':y,'rotationDegrees':angle},'drawOrder':order,'textureBinding':binding}
        artwork[bid] = image
    add('torso',None,*ORIGIN,-90,110,'profile-torso' if profile else 'torso',120,(.5,.92),20,90)
    add('head','torso',110,0,0,125,'profile-head' if profile else 'head',145,(.48,.96),30,90)
    for side, sign in [('left',-1),('right',1)]:
        far = side == 'left'
        shoulder = sign*(10 if profile else 35)
        add(side+'_upper_arm','torso',75,shoulder,180,55,'upper-arm',66,(.5,.08),40 if far else 43,-90)
        add(side+'_forearm',side+'_upper_arm',55,0,0,55,'forearm',66,(.5,.08),41 if far else 44,-90)
        add(side+'_hand',side+'_forearm',55,0,-90,20,'hand-rest',35,(.5,.08),42 if far else 45)
        hand=bones[side+'_hand']; hand['artworkVariants']={}
        for variant in ['palm','point','fist']:
            image=images['hand-'+variant]
            address=side+'_hand#'+variant
            hand['artworkVariants'][variant]={'name':variant.title(),'binding':{
                'assetId':address,'pivotX':image.width*.48,'pivotY':image.height*.91,
                'scale':38/image.height,'rotationDegrees':180}}
            # Alternate hands have wrists at the bottom: orient upward, not down like rest.
            artwork[address]=image
        add(side+'_thigh','torso',0,sign*(5 if profile else 16),180,90,'thigh',105,(.5,.05),0 if far else 8,-90)
        add(side+'_shin',side+'_thigh',90,0,0,90,'shin',103,(.5,.05),1 if far else 9,-90)
        bones[side+'_shin']['ikChainRootId']=side+'_thigh'
        add(side+'_foot',side+'_shin',90,0,-90,25,'foot',24,(.28,.2),2 if far else 10)
    prefix='profile-' if profile else ''
    # Face anchors are bone-local; the head's -90 degree orientation maps (up,right) to (x,y).
    eye_x, mouth_x = (38,45) if profile else (10,14)
    add('eyes','head',75,eye_x,90,1,prefix+'eyes-rest',30,(.5,.5),60)
    add('mouth','head',44,mouth_x,90,1,'mouth-rest',3,(.5,.5),61)
    for bid, variants in [('eyes',['angry','laugh','surprise']),('mouth',['open','frown'])]:
        bones[bid]['artworkVariants']={}
        base_scale = bones[bid]['textureBinding']['scale']
        for variant in variants:
            name=(prefix if bid=='eyes' else '')+bid+'-'+variant
            image=images[name]; address=bid+'#'+variant
            # Preserve expression size deliberately: tiny closed mouths must not expand to open-mouth height.
            scale=base_scale if bid=='eyes' else (22/image.height if variant=='open' else 3/image.height)
            bones[bid]['artworkVariants'][variant]={'name':variant.title(),'binding':{
                'assetId':address,'pivotX':image.width/2,'pivotY':image.height/2,'scale':scale}}
            artwork[address]=image
    from street_skin import attach_continuous_limbs
    attach_continuous_limbs({'bones':bones}, artwork, continuous)
    aid='ls-'+character+'-street-v2'; view='PROFILE' if profile else 'THREE_QUARTER'; rid=aid+'-'+view.lower()
    revision={'schemaVersion':1,'actorId':aid,'revisionId':rid,'sceneGraph':{'rootBoneIds':['torso'],'bones':bones},
        'artworkAssetPaths':{address:f'artwork/{rid}/{address.replace("#","--")}.png' for address in artwork},
        'createdAtEpochMs':STAMP,'view':view}
    return revision,artwork


def world_pose(graph, rotations, roots):
    result={}
    def visit(bid):
        bone=graph['bones'][bid]; local=bone['localTransform']; parent=bone['parentId']
        (px,py),pa = visit(parent) if parent else ((0,0),0)
        r=math.radians(pa); offset=roots.get(bid,{'x':0,'y':0}) if not parent else {'x':0,'y':0}
        x,y=local['x']+offset['x'],local['y']+offset['y']
        result[bid]=((px+x*math.cos(r)-y*math.sin(r),py+x*math.sin(r)+y*math.cos(r)),pa+rotations[bid])
        return result[bid]
    for bid in graph['bones']:
        if bid not in result: visit(bid)
    return result


def draw_actor(target, revision, images, key, selections, transform):
    """Preview uses the native binding's scale, pivot, offset and compensating rotation, including mirrors."""
    graph=revision['sceneGraph']; pose=world_pose(graph,key['rotations'],key['rootTranslations'])
    sx,sy=transform.get('scaleX',1),transform.get('scaleY',1)
    for bid,bone in sorted(graph['bones'].items(),key=lambda item:item[1]['drawOrder']):
        variant=selections.get(bid,''); address=bid+'#'+variant if variant else bid
        binding=bone.get('artworkVariants',{}).get(variant,{}).get('binding',bone['textureBinding'])
        source=images[address]; (px,py),angle=pose[bid]
        r=math.radians(angle+binding.get('rotationDegrees',0)); scale=binding.get('scale',1)
        a,b,c,d=math.cos(r)*scale*sx,-math.sin(r)*scale*sx,math.sin(r)*scale*sy,math.cos(r)*scale*sy
        tx=transform['x']+px*sx-a*binding['pivotX']-b*binding['pivotY']
        ty=transform['y']+py*sy-c*binding['pivotX']-d*binding['pivotY']
        corners=[(a*u+b*v+tx,c*u+d*v+ty) for u,v in [(0,0),(source.width,0),(0,source.height),(source.width,source.height)]]
        left=math.floor(min(q[0] for q in corners));top=math.floor(min(q[1] for q in corners))
        width=math.ceil(max(q[0] for q in corners))-left;height=math.ceil(max(q[1] for q in corners))-top
        determinant=a*d-b*c; ia,ib,ic,idv=d/determinant,-b/determinant,-c/determinant,a/determinant
        ox,oy=left-tx,top-ty
        patch=source.transform((width,height),Image.Transform.AFFINE,(ia,ib,ia*ox+ib*oy,ic,idv,ic*ox+idv*oy),Image.Resampling.BICUBIC)
        target.alpha_composite(patch,(left,top))
