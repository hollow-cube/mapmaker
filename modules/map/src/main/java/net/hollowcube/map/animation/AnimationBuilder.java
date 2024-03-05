package net.hollowcube.map.animation;

public class AnimationBuilder {
//    private final EditingMapWorld world;
//    private final EventNode<InstanceEvent> eventNode;
//    private Audience audience = Audience.empty();
//
//    private String name = null;
//    private List<AnimatorV2> animators = new ArrayList<>();
//
//    private int tick = 0;
//
//    private Task task = null;
//
//    public static AnimationBuilder instance;
//
//    public AnimationBuilder(@NotNull EditingMapWorld world) {
//        instance = this;
//        this.world = world;
//        var lis = new HCModListener();
//        this.eventNode = EventNode.type("animation_builder", EventFilter.INSTANCE, this::isEventRelevant)
//                .addListener(TerraformSpawnEntityEvent.class, this::handleEntitySpawn)
//                .addListener(TerraformMoveEntityEvent.class, this::handleEntityMove)
//                .addListener(TerraformModifyEntityEvent.class, this::handleEntityModified)
//                .addListener(PlayerPluginMessageEvent.class, lis::handlePluginMessage)
//                .addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);
//        world.addScopedEventNode(eventNode);
//    }
//
//    public void seek(int tick, boolean needsSync) {
//        var wasPlaying = task != null;
//        if (wasPlaying) {
//            pause();
//            world.instance().scheduleNextTick(instance -> {
//                seek(tick, false);
//                play();
//            });
//            return;
//        }
//
//        this.tick = tick;
//        animators.forEach(animator -> {
//            animator.seek(tick);
//            animator.sync();
//        });
//        if (needsSync) {
//            PacketUtils.sendPacket(audience, new HCSetAnimationStatePacket(tick, 1f));
//        }
//    }
//
//    public void play() {
//        play(1);
//    }
//
//    public void play(int delay) {
//        if (task != null) return; // Already playing
//
//        this.task = world.instance().scheduler().scheduleTask(this::tick, TaskSchedule.tick(delay), TaskSchedule.tick(1));
//    }
//
//    public void pause() {
//        if (task == null) return; // Already paused
//
//        task.cancel();
//        task = null;
//
//        seek(tick, false);
//    }
//
//    public void step() {
//        seek(tick + 1, false);
//    }
//
//    private void tick() {
//        tick++;
//        animators.forEach(Animator::tick);
//    }
//
//    public void begin(@NotNull String name) {
//        this.name = name;
////        this.currentFrame = new KeyFrame();
////        frames.add(currentFrame);
//
//        for (var player : world.players()) {
//            var actionbar = ActionBar.forPlayer(player);
//            actionbar.addProvider(new ActionBarProvider());
//        }
//
//        this.audience = world.instance();
//    }
//
//    public boolean isActive() {
//        return name != null;
//    }
//
//    private boolean isEventRelevant(@NotNull InstanceEvent event, @NotNull Instance instance) {
//        return isActive() && world.instance().equals(instance);
//    }
//
//    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
//        var player = event.getPlayer();
//
//        var actionbar = ActionBar.forPlayer(player);
//        actionbar.addProvider(new ActionBarProvider());
//
//        // Send the existing data to them
//        for (var animator : animators) {
//            PacketUtils.sendPacket(player, new HCUpdateAnimationDataPacket(animator.createAddObjectEntry()));
//
//        }
//    }
//
//    private void handleEntitySpawn(@NotNull TerraformSpawnEntityEvent event) {
//        var animator = new AnimatorV2(world.instance(), event.getEntity(), tick);
//        event.setEntity(animator.getEntity());
//        this.animators.add(animator);
//
//        // Add an initial keyframe with the position
//        animator.keyframe(Properties.POSITION).setValue(event.getPosition());
//        buildParticleData();
//
//        PacketUtils.sendPacket(audience, new HCUpdateAnimationDataPacket(animator.createAddObjectEntry()));
//
//        audience.sendMessage(Component.text("Added entity " + event.getEntity().getUuid()));
//    }
//
//    private void handleEntityMove(@NotNull TerraformMoveEntityEvent event) {
//        var animator = getAnimatorForEntity(event.getEntity());
//        if (animator == null) return;
//
//        animator.keyframe(Properties.POSITION).setValue(event.getNewPosition());
//        buildParticleData();
//        PacketUtils.sendPacket(audience, new HCUpdateAnimationDataPacket(
//                new HCUpdateAnimationDataPacket.UpdateProperty(animator.getEntity().getUuid(), animator.getKeyframes().get(Properties.POSITION))
//        ));
//    }
//
//    private void handleEntityModified(@NotNull TerraformModifyEntityEvent event) {
//        var animator = getAnimatorForEntity(event.getEntity());
//        if (animator == null) return;
//
//        var entityMeta = event.getEntity().getEntityMeta();
//        if (entityMeta instanceof AbstractDisplayMeta meta) {
//            assignIfChanged(animator, Properties.SCALE, meta.getScale());
//        }
//    }
//
//    private <T> void assignIfChanged(@NotNull AnimatorV2 animator, @NotNull Property<T> property, @NotNull T newValue) {
//        boolean isDefaultValue = property.defaultValue().equals(newValue);
//
//        // Never assign if the property is equal to the default value and the animator doesn't have the property already
//        if (isDefaultValue && !animator.hasProperty(property))
//            return;
//
//        // Get the closest keyframe to check if the value is already set
//        var closest = animator.keyframe(property, false);
//        if (newValue.equals(closest.value())) return;
//
//        // The value seems to have changed, so we should assign it
//        animator.keyframe(property).setValue(newValue);
//        buildParticleData();
//        PacketUtils.sendPacket(audience, new HCUpdateAnimationDataPacket(
//                new HCUpdateAnimationDataPacket.UpdateProperty(animator.getEntity().getUuid(), animator.getKeyframes().get(property))
//        ));
//    }
//
//    private @Nullable AnimatorV2 getAnimatorForEntity(@NotNull Entity entity) {
//        for (var animator : animators) {
//            if (animator.getEntity().equals(entity)) {
//                return animator;
//            }
//        }
//        return null;
//    }
//
//    private List<ParticlePacket> particles = List.of();
//
//    private void buildActionBar(@NotNull Player player, @NotNull FontUIBuilder builder) {
//        builder.append("Current Tick: " + tick);
//
//        particles.forEach(player::sendPacket);
//    }
//
//    public void sendDebugInfo(@NotNull Player player) {
//        if (!isActive()) {
//            player.sendMessage("Animation builder is not active");
//            return;
//        }
//
//        var builder = Component.text("name: " + name).appendNewline()
//                .append(Component.text("tick: " + tick)).appendNewline()
//                .append(Component.text("animators: " + animators.size())).appendNewline();
//
//        for (var animator : animators) {
//            builder = builder.append(Component.text("  - " + animator.getEntity().getUuid())).appendNewline();
//            for (var entry : animator.getKeyframes().entrySet()) {
//                builder = builder.append(Component.text("  " + entry.getKey())).appendNewline();
//                for (var keyframe : entry.getValue().keyframes) {
//                    builder = builder.append(Component.text("    - " + keyframe.time() + ": " + keyframe.value())).appendNewline();
//                }
//            }
//        }
//
//        player.sendMessage(builder);
//
//        buildParticleData();
//
////        player.sendMessage("current: " + currentFrame);
//    }
//
//    private void buildParticleData() {
//        var particles = new ArrayList<ParticlePacket>();
//
//        for (var animator : animators) {
//            var points = new ArrayList<Point>();
//
//            var positionFrames = animator.getKeyframes().get(Properties.POSITION);
//            if (positionFrames == null) continue;
//
//            for (var keyframe : positionFrames.keyframes) {
//                points.add((Pos) keyframe.value());
//            }
//
//            for (int i = 0; i < points.size() - 1; i++) {
//                var from = points.get(i);
//                var to = points.get(i + 1);
//
//                particles.addAll(createParticleLine(from, to, 0.1f, 0xFF0000));
//            }
//        }
//
//        this.particles = particles;
//    }
//
//    private @NotNull List<ParticlePacket> createParticleLine(@NotNull Point from, @NotNull Point to, float size, int color) {
//        var particles = new ArrayList<ParticlePacket>();
//
//        var distance = from.distance(to);
//        var direction = Vec.fromPoint(to.sub(from)).normalize();
//        var step = 0.25f;
//
//        for (float i = 0; i < distance; i += step) {
//            var point = from.add(direction.mul(i));
//            particles.add(ParticleCreator.createParticlePacket(
//                    Particle.DUST, true,
//                    point.x(), point.y(), point.z(),
//                    0, 0, 0,
//                    0, 1, buffer -> {
//                        buffer.writeFloat(255f / 255f);
//                        buffer.writeFloat(1f / 255f);
//                        buffer.writeFloat(1f / 255f);
//                        buffer.writeFloat(0.5f);
//                    }
//            ));
//        }
//
//        return particles;
//    }
//
//
//    private class ActionBarProvider implements ActionBar.Provider {
//        @Override
//        public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
//            buildActionBar(player, builder);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            return obj instanceof ActionBarProvider;
//        }
//
//        @Override
//        public int hashCode() {
//            return ActionBarProvider.class.hashCode();
//        }
//    }
}
