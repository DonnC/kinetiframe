"""Authored acting beats and contact-aware locomotion, in actor-local pixels and seconds."""
import math
from street_actor import ORIGIN, world_pose

FPS=24
SECONDS=20
SCALE=.85


def ease(t):
    t=max(0.,min(1.,t)); return t*t*(3-2*t)


def beat(t,start,hold,end):
    return ease((t-start)/.45)*(1-ease((t-hold)/(end-hold)))


def foot(t,side,speed=105.,period=1.):
    # Stance uses a fixed world-space anchor, NOT a leg sinusoid that slides along the floor.
    offset=0. if side=='left' else .5
    phase=t/period+offset; cycle=math.floor(phase); u=phase-cycle; stride=speed*period
    anchor=ORIGIN[0]+(cycle-offset+.3)*stride
    if u<.6: return anchor,ORIGIN[1]+174,True
    swing=(u-.6)/.4
    return anchor+stride*ease(swing),ORIGIN[1]+174-35*math.sin(math.pi*swing)**2,False


def pose(revision,t,role):
    graph=revision['sceneGraph']; rotations={bid:b['localTransform']['rotationDegrees'] for bid,b in graph['bones'].items()}
    walking=role in ['approach','exit']; roots={'torso':{'x':0.,'y':-3+1.5*math.sin(t*2*math.pi/3.5)}}
    contacts={}; selections={}
    if walking:
        # Approach spans exactly six steps. Slow the final step with a monotonic clock remap.
        clock=t if role=='exit' else (t if t<5 else 5+(t-5)-(t-5)**2/2)
        speed=105.; roots['torso']={'x':clock*speed,'y':4-3*math.cos(clock*4*math.pi)}
        rotations['torso']=-86 if role=='exit' else -88
        rotations['head']=-4 if role=='exit' else -2
        amplitude=24*(1-ease((t-5)/1)) if role=='approach' else 24
        for side,sign in [('left',-1),('right',1)]:
            rotations[side+'_upper_arm']=180+sign*amplitude*math.sin(clock*2*math.pi)
            rotations[side+'_forearm']=-15+sign*8*math.cos(clock*2*math.pi)
        if role=='exit': selections={'eyes':'angry','mouth':'frown','left_hand':'fist','right_hand':'fist'}
    else:
        # Torso leads, then head and wrist follow. Asymmetry makes a gesture read as acting, not a metronome.
        if role=='mira':
            explain=beat(t,.4,2.4,3.5); welcome=beat(t,6,7.5,8.7); answer=beat(t,9,10.7,12)
            rotations['torso']=-90+3*explain-2*welcome
            rotations['head']=-3*beat(t,.6,2.5,3.6)+4*beat(t,6.2,7.5,8.9)
            rotations['right_upper_arm']=180-65*explain-55*answer
            rotations['right_forearm']=-50*explain-62*answer
            rotations['right_hand']= -90+22*explain
            rotations['left_upper_arm']=180+75*welcome
            rotations['left_forearm']=45*welcome
            rotations['left_hand']=-90-20*welcome
            selections['right_hand']='palm' if max(explain,answer)>.15 else ''
            selections['left_hand']='palm' if welcome>.15 else ''
            if 1<t<3.2 or 9<t<11.5: selections['mouth']='open' if int(t*5)%3 else ''
            if 12.4<t<15.5: selections.update(eyes='surprise',mouth='')
            if t>16: selections.update(eyes='laugh',mouth='open')
        elif role=='tavi':
            hello=beat(t,.2,1.2,2.3); talk=beat(t,3,4.6,6)
            rotations['torso']=-90-2*hello+3*talk
            rotations['head']=3*beat(t,.4,1.4,2.5)-3*beat(t,3.2,4.7,6.2)
            rotations['right_upper_arm']=180-85*hello-50*talk
            rotations['right_forearm']=-50*hello-65*talk
            rotations['right_hand']=-90+25*hello
            selections['right_hand']='palm' if max(hello,talk)>.12 else ''
            if .5<t<1.7 or 3<t<5.8: selections['mouth']='open' if int(t*5)%3 else ''
            if 6.5<t<9: selections.update(eyes='surprise',mouth='')
            if t>10: selections.update(eyes='laugh',mouth='open')
        elif role=='kuda':
            reply=beat(t,3,4.5,5.8); objecting=beat(t,9,10.4,11.5); anger=ease((t-11.8)/.9)
            rotations['torso']=-90+3*reply-4*anger
            rotations['head']=-3*beat(t,3.2,4.7,6)-7*anger
            rotations['right_upper_arm']=180-60*reply-75*objecting+15*anger
            rotations['right_forearm']= -55*reply-40*objecting-12*anger
            rotations['left_upper_arm']=180-15*anger
            rotations['left_forearm']=-20*anger
            selections['right_hand']='point' if objecting>.2 else 'palm' if reply>.2 else 'fist' if anger>.5 else ''
            selections['left_hand']='fist' if anger>.5 else ''
            if 3<t<5.4 or 9<t<11.4: selections['mouth']='open' if int(t*5)%3 else ''
            if anger>.4: selections.update(eyes='angry',mouth='frown')
    # A brief blink is an artwork swap, not scaling the whole head or replacing a raster frame.
    if not walking and any(abs(t-b)<.07 for b in [2.8,6.2,10.9,17.1]) and selections.get('eyes','')=='': selections['eyes']='laugh'
    world=world_pose(graph,rotations,roots)
    for side,sign in [('left',-1),('right',1)]:
        (hx,hy),parent_angle=world[side+'_thigh']
        directional = any(f'all-views-v{version}' in revision['actorId'] for version in (2, 3))
        standing = (hx, ORIGIN[1]+178.8, True) if directional else (ORIGIN[0]+sign*16,ORIGIN[1]+174,True)
        tx,ty,planted=foot(clock,side) if walking else standing
        dx,dy=tx-hx,ty-hy; distance=min(179.99,max(.1,math.hypot(dx,dy)))
        direction=math.atan2(dy,dx); bend=1 # The knee leads the ankle toward the walking direction.
        upper=direction-bend*math.acos(distance/180)
        knee=math.acos((distance*distance-90*90-90*90)/(2*90*90))*bend
        torso_angle=world['torso'][1]
        rotations[side+'_thigh']=math.degrees(upper)-torso_angle
        rotations[side+'_shin']=math.degrees(knee)
        rotations[side+'_foot']=-math.degrees(upper+knee)
        if planted:
            contacts[side+'_shin']={'x':tx,'y':ty,'bendSide':-bend,'followBoneId':side+'_foot','followWorldAngle':0}
    return {'timeMicros':round(t*1e6),'rotations':rotations,'rootTranslations':roots,'contacts':contacts},selections


def performance(revision,role,duration,project_id):
    from street_actor import STAMP
    keys=[]; artwork=[]; last=None
    for frame in range(round(duration*FPS)+1):
        key,selections=pose(revision,frame/FPS,role); keys.append(key)
        # Record complete selections on change, including resets. Each key stays independently readable.
        selections={bid:selections.get(bid,'') for bid in ['eyes','mouth','left_hand','right_hand']}
        if selections!=last:
            artwork.append({'timeMicros':key['timeMicros'],'selections':selections}); last=selections
    return {'schemaVersion':6,'performanceId':revision['actorId']+'-'+role,'projectId':project_id,
        'actorId':revision['actorId'],'actorRevisionId':revision['revisionId'],
        'displayName':revision['actorId'].split('-')[1].title()+' · '+role.title(),
        'durationMicros':round(duration*1e6),'playback':'END_HOLD','poseInterpolation':'SMOOTH',
        'poseKeys':keys,'artworkKeys':artwork,'createdAtEpochMs':STAMP,'updatedAtEpochMs':STAMP}
