# LG webOS Voice Search protocol findings

## Decision

**NOT SUPPORTED BY AVAILABLE LG WEBOS REMOTE API:** The public Connect SDK capability surface does not expose a microphone audio stream, a Magic Remote Voice-button press/release command, or a system Voice Assistant launch operation.

The application therefore uses the practical fallback:

```text
Android SpeechRecognizer → recognized text → existing TextInputControl → Enter
```

This fallback works only while webOS exposes an active text input field. It must not be reported as direct LG System Voice Assistant support.

## Evidence

- LG Electronics' official Connect SDK `TextInputControl` capability defines text-input operations and subscription to text-input status. It is the public boundary used by this project:  
  https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/capability/TextInputControl.java
- The official Connect SDK Android Core capability package exposes application launch, key control, mouse control, text input, TV control and volume control. No Voice/Microphone capability is defined:  
  https://github.com/ConnectSDK/Connect-SDK-Android-Core/tree/master/src/com/connectsdk/service/capability
- Official webOS TV service implementation used by Connect SDK:  
  https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/WebOSTVService.java
- Connect SDK capability documentation for webOS TV services:  
  https://connectsdk.com/en/latest/apis-and/and-webostvservice.html

## Current repository integration

The app already reuses one connected `Device` and does not open a second LG connection:

- `LgDevice.sendText` delegates to the connected webOS text-input capability.
- `LgDevice.sendEnter` delegates to the same text-input capability.
- `LgDevice.launchApp` and `executeControllerButton` use the existing service.
- Voice recognition is local to Android and does not stream audio to the TV.

## Model context

The physical test model reported by the user is `UM7400PTAfr`. The current discovery/domain model does not expose a reliable webOS firmware version, so no model-specific private protocol is assumed.

## Safety rule

Do not add `sendVoiceSearch()`, a guessed `VOICE` special key, or an undocumented Luna-service URI unless a future official LG API/source explicitly defines it and the target TV confirms it. Application-specific search navigation (YouTube, Netflix, Browser) must not be assumed to share one protocol.
