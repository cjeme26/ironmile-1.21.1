# Use-key right-click shifter fix

This version stops assuming a GLFW mouse button.

- The shifter now watches Minecraft's actual Use Item / Place Block keybinding.
- The vanilla MinecraftClient.doItemUse() action is cancelled while that input
  belongs to a manual IronMile shifter.
- Mouse look is captured whenever the Use key is held in a manual car.
- R/F remain available as fallback shift controls.
- A temporary yellow "SHIFT INPUT" label appears while the shifter believes it
  is receiving the Use key. This is only for confirming the input path works.
