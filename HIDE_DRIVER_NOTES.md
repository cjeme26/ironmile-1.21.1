# Hide driver model patch

This client-side mixin prevents player models from rendering while they are passengers of an Iron Mile `CarEntity`.

It does not change:
- player or vehicle hitboxes
- passenger state
- networking
- vehicle controls
- server behavior

The player model becomes visible again immediately after dismounting.
