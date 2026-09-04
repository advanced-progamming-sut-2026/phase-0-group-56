package models.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

public class AudioManager implements Disposable {
    private static AudioManager instance;

    private ObjectMap<String, Music> musicLibrary = new ObjectMap<>();
    private ObjectMap<String, Sound> sfxLibrary = new ObjectMap<>();

    private Music currentMusic;
    private float musicVolume = 0.8f;
    private float sfxVolume = 0.8f;

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    // Load everything at once
    public void loadAll() {
        // Music
        loadMusic("ancient_egypt", "audio/music/ancient egypt chapter.mp3");
        loadMusic("big_wave_beach", "audio/music/big wave beach chapter.mp3");
        loadMusic("dark_ages", "audio/music/dark ages chapter.mp3");
        loadMusic("frostbite_caves", "audio/music/frostbite caves chapter.mp3");

        // SFX
        loadSFX("explosion", "audio/sfx/explosion audio.mp3");
        loadSFX("lawnmower", "audio/sfx/lownmower.mp3");
        loadSFX("win", "audio/sfx/win audio.mp3");
    }

    private void loadMusic(String name, String path) {
        if (!musicLibrary.containsKey(name)) {
            musicLibrary.put(name, Gdx.audio.newMusic(Gdx.files.internal(path)));
        }
    }

    private void loadSFX(String name, String path) {
        if (!sfxLibrary.containsKey(name)) {
            sfxLibrary.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
        }
    }

    // ============ MUSIC ============

    public void playMusic(String name) {
        if (currentMusic != null) currentMusic.stop();

        Music music = musicLibrary.get(name);
        if (music == null) {
            System.err.println("Music not found: " + name);
            return;
        }

        currentMusic = music;
        currentMusic.setLooping(true);
        currentMusic.setVolume(musicVolume);
        currentMusic.play();
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    // ============ SFX ============

    public void play(String name) {
        Sound sound = sfxLibrary.get(name);
        if (sound == null) {
            System.err.println("SFX not found: " + name);
            return;
        }
        sound.play(sfxVolume);
    }

    // ============ VOLUME ============

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        if (currentMusic != null) currentMusic.setVolume(volume);
    }

    public void setSFXVolume(float volume) {
        sfxVolume = volume;
    }

    @Override
    public void dispose() {
        for (Music m : musicLibrary.values()) m.dispose();
        for (Sound s : sfxLibrary.values()) s.dispose();
        musicLibrary.clear();
        sfxLibrary.clear();
    }
}
