# Media session callbacks

> [GitHub 链接](https://github.com/Yjnull/android-interview-planing/blob/master/questions/Audio%26Video/Media_App_Architecture/Building_an_audio_app/Media_session_callbacks.md)
> 译：https://developer.android.com/guide/topics/media-apps/audio-app/mediasession-callbacks

你的 media session callbacks 调用多个 API 中的方法来控制播放器，管理音频焦点，以及与 media session 和 media browser service 通信。下表总结了这些任务如何在回调中分布。

|          | onPlay() | onPause() | onStop() |
| -------  | -------  |  -------  | -------  | 
| [Audio Focus](https://developer.android.com/guide/topics/media-apps/audio-focus) | `requestFocus()` passing in your `OnAudioFocusChangeListener`.Always call `requestFocus()` first, proceed only if focus is granted. |  | `abandonAudioFocus()` |
| [Service](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice)  | `startService()` |   | `stopSelf()`  |
| [Media Session](https://developer.android.com/guide/topics/media-apps/working-with-a-media-session) | `setActive(true)`<br>- Update metadata and state | -Update metadata and state | `setActive(false)`<br> -Update metadata and state |
| Player Implementation | Start the player | Pause the player | Stop the player |
| [Becoming Noisy](https://developer.android.com/guide/topics/media-apps/volume-and-earphones#becoming-noisy) | Register your `BroadcastReceiver` | Unregister your BroadcastReceiver	| |
| [Notifications](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice#mediastyle-notifications)  | `startForeground(notification)` | `stopForeground(false)` | `stopForeground(false)`  |

以下是 callback 的示例框架：

```java
private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

// Defined elsewhere...
private AudioManager.OnAudioFocusChangeListener afChangeListener;
private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
private MediaStyleNotification myPlayerNotification;
private MediaSessionCompat mediaSession;
private MediaBrowserService service;
private SomeKindOfPlayer player;

private AudioFocusRequest audioFocusRequest;

MediaSessionCompat.Callback callback = new
    MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Request audio focus for playback, this registers the afChangeListener
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .setAudioAttributes(attrs)
                    .build();
            int result = am.requestAudioFocus(audioFocusRequest);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start the service
                startService(new Intent(context, MediaBrowserService.class));
                // Set the session active  (and update metadata and state)
                mediaSession.setActive(true);
                // start the player (custom call)
                player.start();
                // Register BECOME_NOISY BroadcastReceiver
                registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
                // Put the service in the foreground, post notification
                service.startForeground(id, myPlayerNotification);
            }
        }

        @Override
        public void onStop() {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Abandon audio focus
            am.abandonAudioFocusRequest(audioFocusRequest);
            unregisterReceiver(myNoisyAudioStreamReceiver);
            // Stop the service
            service.stopSelf();
            // Set the session inactive  (and update metadata and state)
            mediaSession.setActive(false);
            // stop the player (custom call)
            player.stop();
            // Take the service out of the foreground
            service.stopForeground(false);
        }

        @Override
        public void onPause() {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Update metadata and state
            // pause the player (custom call)
            player.pause();
            // unregister BECOME_NOISY BroadcastReceiver
            unregisterReceiver(myNoisyAudioStreamReceiver);
            // Take the service out of the foreground, retain the notification
            service.stopForeground(false);
        }
    };
```







