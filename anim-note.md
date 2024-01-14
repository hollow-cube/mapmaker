# Animation builder

```
/anim create my_name
-- At this point the user is in the builder with a single keyframe at t=0

-- Summon an entity
   1. automatically added to the animation at the current frame as a spawn
   2. chat message telling you it has been added

/anim frame add 20 -> "added frame #2"
-- Adds a new keyframe at t=20 (aka 1 second)

-- Move the entity
  1. move recorded in animation frame

/anim goto 1 -> "moved to frame #1"
-- Moves the entity to the position at frame 1

/anim play -> "playing animation"
-- Plays the animation from the current frame to the end

/anim exit -> "exiting animation builder"



Case Study:
1. create frame 1
2. spawn a villager
3. goto t=20 (1s)
4. move the villager
5. goto t=40 (2s)
6. move the villager

do we need frame add 20, 40 in there? or can it just add a new frame whenever you move the villager?




EACH MOVABLE OBJECT WILL HAVE ITS OWN SET OF KEYFRAMES
- Entity1 -> t=1,xyz=1,2,3 -> t=2,xyz=2,3,4
- Entity1 -> t=1,spawn -> t=2,xyz=1,2,3 -> t=3,xyz=2,3,4


```

