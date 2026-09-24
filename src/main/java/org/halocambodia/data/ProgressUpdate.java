package org.halocambodia.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Broadcast payload for per-video progress updates. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdate {
    private String videoId;
    private String title;
    private double progress;  // 0.0..1.0
    private String status;    // e.g. "Downloading 47%", "Merging", "Done", "Skipped"
}
