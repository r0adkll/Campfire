package app.campfire.shake

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import platform.CoreMotion.CMMotionManager

class ShakeDetectorUIViewControllerDelegate : ComposeUIViewControllerDelegate {
  val motionManager = CMMotionManager()

  override fun viewDidLoad() {
    super.viewDidLoad()

  }
}
