package utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static AudioManager instance;
    private final Map<String, Sound> soundMap = new HashMap<>();

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    public void loadSounds() {
        soundMap.put("notification", Gdx.audio.newSound(Gdx.files.internal("sounds/notification.wav")));
        soundMap.put("banana", Gdx.audio.newSound(Gdx.files.internal("sounds/banana.wav")));
        soundMap.put("shout", Gdx.audio.newSound(Gdx.files.internal("sounds/shout.wav")));
        soundMap.put("dance", Gdx.audio.newSound(Gdx.files.internal("sounds/dance.wav")));
    }

    public void playSound(String id) {
        Sound s = soundMap.get(id);
        if (s != null) s.play();
    }

    public void dispose() {
        for (Sound s : soundMap.values()) s.dispose();
        soundMap.clear();
    }
}
