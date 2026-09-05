# 1.3.0

- Minecraft 26.2 support
- Minecraft 26.1.2 support (Java 25).
- Modernize package and maven group: Java package `net.rpg_foundation.structure_pool` (was `net.fabric_extras.structure_pool`), maven group `net.rpg_foundation` (was `net.foundation`); mod id and data unchanged
- **Spawn limits are now enforced for elements reached through a pool's fallback pool.** They were silently uncapped before. **This is seed-visible**: filtering an element out of a candidate list changes what `Util.shuffle` returns, so an affected structure generates differently for the same seed. Only structures whose placement reaches an injected element via a fallback pool are affected.
- No longer ships an access widener (Fabric) / access transformer (NeoForge) — one fewer thing to conflict with.
- One fewer mixin: `StructureTemplatePool` is no longer mixed into at all (injected elements are now identified by instance identity), and all mixin-owned members are `@Unique`.
- `injectIntoStructurePool` is idempotent per pool instance: injecting the same structure into the same pool twice no longer doubles its weight, and warns instead.
- Injecting now invalidates the pool's memoised `maxSize`.
- The "structure pool not found" message goes to the mod logger instead of `System.err`, so a mistyped pool id in a config is visible in a filtered log.

# 1.2.1

- Fix structures not generating second first world load

# 1.2.0

- Migrate to Architectury

# 1.1.3

- Lower Fabric API version requirement

# 1.1.2

- Allow running on 1.21

# 1.1.1

- Update to Minecraft 1.21.1

# 1.1.0

- Update to Minecraft 1.21.

# 1.0.0

Initial release.

#