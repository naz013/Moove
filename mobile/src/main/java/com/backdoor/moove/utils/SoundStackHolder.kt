package com.backdoor.moove.utils

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import timber.log.Timber

class SoundStackHolder(context: Context, val prefs: Prefs) : Sound.PlaybackCallback {

    var sound: Sound? = null
        private set

    private var mMusicVolume = -1
    private var mAlarmVolume = -1
    private var mNotificationVolume = -1

    private var isDoNotDisturbEnabled: Boolean = false
    private var isHeadset: Boolean = false
    private var isIncreasingLoudnessEnabled: Boolean = false
    private var hasDefaultSaved: Boolean = false
    private var hasVolumePermission: Boolean = false

    private var mAudioManager: AudioManager? = null
    private val mHandler = Handler()

    private var mStreamVol: Int = 0
    private var mVolume: Int = 0
    private var mStream: Int = 0
    private var mMaxVolume: Int = 0

    private val mVolumeIncrease = object : Runnable {
        override fun run() {
            Timber.d("mVolumeIncrease -> run: $mVolume, $mStreamVol")
            if (mVolume < mStreamVol) {
                mVolume++
                mHandler.postDelayed(this, 750)
                mAudioManager?.setStreamVolume(mStream, mVolume, 0)
            } else
                mHandler.removeCallbacks(this)
        }
    }

    init {
        isHeadset = SuperUtil.isHeadsetUsing(context)
        hasVolumePermission = SuperUtil.hasVolumePermission(context)
        isIncreasingLoudnessEnabled = prefs.isIncreasingLoudnessEnabled
        if (mAudioManager == null) {
            if (sound != null)
                sound?.stop(true)
            else
                sound = Sound(context, prefs)

            sound?.setCallback(this)
            mAudioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (mAudioManager != null && Permissions.checkPermission(context, Permissions.BLUETOOTH))
                mAudioManager?.mode = AudioManager.MODE_NORMAL
            isDoNotDisturbEnabled = SuperUtil.isDoNotDisturbEnabled(context)
        }
    }

    fun setMaxVolume(maxVolume: Int) {
        this.mMaxVolume = maxVolume
    }

    @Synchronized
    private fun saveDefaultVolume() {
        Timber.d("saveDefaultVolume: %s", hasDefaultSaved)
        if (!hasDefaultSaved && mAudioManager != null) {
            mMusicVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
            mAlarmVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: -1
            mNotificationVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: -1
            hasDefaultSaved = true
        }
    }

    @Synchronized
    private fun restoreDefaultVolume() {
        Timber.d("restoreDefaultVolume: $hasDefaultSaved, doNot: $isDoNotDisturbEnabled, am $mAudioManager")
        if (hasDefaultSaved && !isDoNotDisturbEnabled) {
            if (mAudioManager != null) {
                try {
                    mAudioManager?.setStreamVolume(AudioManager.STREAM_ALARM, mAlarmVolume, 0)
                    mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, mMusicVolume, 0)
                    mAudioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mNotificationVolume, 0)
                } catch (ignored: SecurityException) {
                }

            }
            mMusicVolume = -1
            mNotificationVolume = -1
            mAlarmVolume = -1
        }
        hasDefaultSaved = false
    }

    override fun onFinish() {
        cancelIncreaseSound()
        restoreDefaultVolume()
    }

    override fun onStart() {
        saveDefaultVolume()
        setPlayerVolume()
    }

    private fun setPlayerVolume() {
        cancelIncreaseSound()
        if (isHeadset) return
        if (!hasVolumePermission) return
        if (mAudioManager == null) return

        mStream = AudioManager.STREAM_MUSIC

        val volPercent = mMaxVolume.toFloat() / 25
        val maxVol = mAudioManager?.getStreamMaxVolume(mStream) ?: 24
        mStreamVol = (maxVol * volPercent).toInt()
        mVolume = mStreamVol
        if (isIncreasingLoudnessEnabled) {
            mVolume = 0
            mHandler.postDelayed(mVolumeIncrease, 750)
        }
        mAudioManager?.setStreamVolume(mStream, mVolume, 0)
    }

    fun cancelIncreaseSound() {
        mHandler.removeCallbacks(mVolumeIncrease)
    }
}
