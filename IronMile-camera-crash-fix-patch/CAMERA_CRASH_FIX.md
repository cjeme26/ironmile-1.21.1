# Camera mixin crash fix

Replaces direct `@Shadow` declarations for Camera#setPos and Camera#setRotation
with uniquely named `@Invoker` methods. This prevents the release remapper from
producing mixin methods that collide with Minecraft's intermediary camera method
names at startup.
