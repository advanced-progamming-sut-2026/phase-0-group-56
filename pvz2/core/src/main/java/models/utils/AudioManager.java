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
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void loadSounds() {
        soundMap.put("sound_1", Gdx.audio.newSound(Gdx.files.internal("sounds/sound_1.wav")));
        soundMap.put("sound_2", Gdx.audio.newSound(Gdx.files.internal("sounds/sound_2.wav")));
        soundMap.put("sound_3", Gdx.audio.newSound(Gdx.files.internal("sounds/sound_3.wav")));
    }

    public void playSound(String soundId) {
        Sound sound = soundMap.get(soundId);
        if (sound != null) {
            sound.play();
        }
    }

    public void playSound(String soundId, float volume) {
        Sound sound = soundMap.get(soundId);
        if (sound != null) {
            sound.play(volume);
        }
    }

    public void dispose() {
        for (Sound sound : soundMap.values()) {
            sound.dispose();
        }
        soundMap.clear();
    }
}
