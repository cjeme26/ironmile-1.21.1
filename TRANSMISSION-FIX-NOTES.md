# Transmission fix test build

Changes:
- Manual R/F shifting is applied locally and sent to the server on the same input edge.
- Manual selected gear is clearly shown in the driving HUD.
- HUD displays SHIFT -> target gear during the shift transition.
- Manual controls are shown while driving a manual car.
- Automatic full-throttle upshift RPM reduced from 5800 to 4500 RPM.
- Torque now fades smoothly between redline and the rev limiter to prevent limiter surging/jitter.

Suggested checks:
1. Manual: R moves 1->2 and F returns 2->1.
2. Manual: F from 1 reaches N, then R; R walks back through N to 1.
3. Manual: hold first gear to redline; car should stop gaining speed smoothly rather than jerking front/back.
4. Automatic: 1->2 should happen around the high-30 km/h range under full throttle.
5. HUD should visibly show current gear and SHIFT -> target gear.
