/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lizlooney.pitchdetector;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.Manifest.permission.RECORD_AUDIO;
import static org.lizlooney.pitchdetector.audio.AudioSource.SAMPLE_RATE_IN_HZ;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.lizlooney.pitchdetector.audio.AudioAnalyzer;
import org.lizlooney.pitchdetector.audio.AudioSource;
import org.lizlooney.pitchdetector.audio.AudioSource.AudioReceiver;

public final class PitchDetectorActivity extends Activity implements AudioReceiver {
  private static final String LOG_TAG = "PitchDetector_Activity";
  private static final int PERMISSION_REQUEST_CODE = 217;
  // Update the animation every .025 seconds maximum.
  private static final int MAX_UPDATE_TIME_MS = 25;


  private long lastUpdatedTimestamp = -1;
  private PitchAnimation pitchAnimation;
  private LinearLayout pitchAnimationContainer;
  private TextView pitchTextView;
  private AudioSource audioSource;
  private AudioAnalyzer audioAnalyzer;
  private final short[] audioAnalyzerBuffer = new short[AudioAnalyzer.BUFFER_SIZE];
  private int audioAnalyzerBufferOffset;
  private Double previousFrequency;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pitch_detector_activity);

    pitchAnimation = new PitchAnimation();
    pitchAnimationContainer = (LinearLayout) findViewById(R.id.pitch_animation_container);
    pitchTextView = findViewById(R.id.pitch_text_view);

    pitchAnimation.initializeAnimation(pitchAnimationContainer, getSizeForAnimation());
    
    audioSource = new AudioSource();
    audioAnalyzer = new AudioAnalyzer(SAMPLE_RATE_IN_HZ);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PERMISSION_GRANTED) {
      startAudioRecording();
    } else {
      ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
        startAudioRecording();
      }
    }
  }

  private void startAudioRecording() {
    audioSource.registerAudioReceiver(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    audioSource.unregisterAudioReceiver(this);
    lastUpdatedTimestamp = -1;
  }

  // AudioReceiver
  @Override
  public void onReceiveAudio(short[] audioSourceBuffer) {
    int audioSourceBufferOffset = 0;
    while (audioSourceBufferOffset < audioSourceBuffer.length) {
      // Repeat the previous frequency value while we collect and analyze new data.
      if (previousFrequency != null) {
        showPitch(previousFrequency);
      }

      int lengthToCopy =
          Math.min(
              audioSourceBuffer.length - audioSourceBufferOffset,
              audioAnalyzerBuffer.length - audioAnalyzerBufferOffset);
      System.arraycopy(
          audioSourceBuffer,
          audioSourceBufferOffset,
          audioAnalyzerBuffer,
          audioAnalyzerBufferOffset,
          lengthToCopy);
      audioAnalyzerBufferOffset += lengthToCopy;
      audioSourceBufferOffset += lengthToCopy;

      // If audioAnalyzerBuffer is full, analyze it.
      if (audioAnalyzerBufferOffset == audioAnalyzerBuffer.length) {
        Double frequency = audioAnalyzer.detectFundamentalFrequency(audioAnalyzerBuffer);
        if (frequency == null) {
          // Unable to detect frequency, likely due to low volume.
          showPitch(0);
        } else if (isDrasticSpike(frequency)) {
          // Avoid drastic changes that show as spikes in the graph between notes
          // being played on an instrument. If the new value is more than 50%
          // different from the previous value, skip it.
          // Note that since we set previousFrequency to frequency below, we
          // will never skip two consecutive values.
          frequency = null;
        } else {
          showPitch(frequency);
        }
        previousFrequency = frequency;

        // Since we've analyzed that buffer, set the offset back to 0.
        audioAnalyzerBufferOffset = 0;
      }
    }
  }

  private boolean isDrasticSpike(double frequency) {
    return previousFrequency != null
        && Math.abs(frequency - previousFrequency) / previousFrequency > 0.50;
  }

  private void showPitch(double frequency) {
    runOnUiThread(() -> {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastUpdatedTimestamp + MAX_UPDATE_TIME_MS) {
          return;
        }

        pitchAnimation.updateAnimation(pitchAnimationContainer, frequency);
        if (frequency != 0) {
          pitchTextView.setText(String.format("%.0f Hz", frequency));
        } else {
          pitchTextView.setText("");
        }
        lastUpdatedTimestamp = timestamp;
    });
  }

  private int getSizeForAnimation() {
    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    return Math.min(size.x, size.y);
  }
}
