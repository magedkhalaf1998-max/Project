# Android app

This is a native Android Java app. Open this directory in Android Studio (with JDK 17 installed), allow Gradle sync, then run `app` on an Android 6.0+ device or emulator.

The app lets the user pick a saved question image using Android's document picker, preview it, and confirm before sending it to the OpenAI API. It does not capture the display in the background. The API key is entered at runtime and is not stored by the app. Do not distribute this demo with a hard-coded API key; production apps should use a secure backend.

You need an OpenAI API key with access to a vision-capable chat-completions model. The default is `gpt-4o-mini`; you can change the model field in the app if your account uses a different model.
