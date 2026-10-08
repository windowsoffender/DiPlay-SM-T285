package com.shilapi.xcertplay.compat

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build

/** A streaming [AudioTrack]. [AudioTrack.Builder] needs API 23; before that the same constructor it calls. */
fun streamAudioTrack(attributes: AudioAttributes, format: AudioFormat, bufferBytes: Int): AudioTrack =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build()
    } else {
        AudioTrack(attributes, format, bufferBytes, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

/** A 16-bit PCM recorder. [AudioRecord.Builder] needs API 23; before that the plain constructor. */
fun pcm16AudioRecord(source: Int, sampleRate: Int, channelMask: Int, bufferBytes: Int): AudioRecord =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()
    } else {
        AudioRecord(source, sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT, bufferBytes)
    }

/** Blocking write. The write mode needs API 23; before that a streaming write always blocks. */
fun AudioTrack.writeBlocking(data: ByteArray, offset: Int, length: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        write(data, offset, length, AudioTrack.WRITE_BLOCKING)
    } else {
        write(data, offset, length)
    }

/** Blocking read. The read mode needs API 23; before that a read always blocks. */
fun AudioRecord.readBlocking(data: ByteArray, offset: Int, length: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        read(data, offset, length, AudioRecord.READ_BLOCKING)
    } else {
        read(data, offset, length)
    }

/** Underruns since the track started. API 24 added the count; earlier releases report 0. */
val AudioTrack.underrunCountCompat: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) underrunCount else 0

/** The buffer the track allocated. API 23 added the query; before that the [requested] size. */
fun AudioTrack.bufferSizeInFramesCompat(requested: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bufferSizeInFrames else requested

/** The type of the device the track plays on, or null before API 23. */
val AudioTrack.routedDeviceType: Int?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) routedDevice?.type else null

/** The type of the device the recorder captures from, or null before API 23. */
val AudioRecord.routedDeviceType: Int?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) routedDevice?.type else null
