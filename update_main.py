import re

with open("app/src/main/java/com/smartparentlock/MainActivity.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Replace startActivity( with startInternalActivity(
content = content.replace("startActivity(", "startInternalActivity(")
# Replace startActivityForResult( with startInternalActivityForResult(
content = content.replace("startActivityForResult(", "startInternalActivityForResult(")

# Now we need to define the isNavigatingInternal variable and the new methods.
# We'll inject it inside the MainActivity class.
# Find class MainActivity : AppCompatActivity() {
class_def = "class MainActivity : AppCompatActivity() {"
methods = """class MainActivity : AppCompatActivity() {

    private var isNavigatingInternal = false

    private fun startInternalActivity(intent: android.content.Intent) {
        isNavigatingInternal = true
        super.startActivity(intent)
    }

    private fun startInternalActivityForResult(intent: android.content.Intent, requestCode: Int) {
        isNavigatingInternal = true
        super.startActivityForResult(intent, requestCode)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isNavigatingInternal && !isFinishing && mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            mInterstitialAd = null
        }
    }
"""

content = content.replace(class_def, methods)

# Also reset in onResume
on_resume = "override fun onResume() {"
on_resume_new = """override fun onResume() {
        isNavigatingInternal = false"""
content = content.replace(on_resume, on_resume_new)

with open("app/src/main/java/com/smartparentlock/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("Updated MainActivity.kt successfully!")
