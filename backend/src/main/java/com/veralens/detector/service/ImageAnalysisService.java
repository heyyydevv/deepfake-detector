package com.veralens.detector.service;

import com.veralens.detector.model.AnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ========================================================
 *  ImageAnalysisService
 *  Rule-Based Deepfake Detection Engine
 * ========================================================
 *
 *  Four independent forensic rules:
 *
 *  1. PIXEL INTENSITY VARIANCE
 *     Computes grayscale variance across the image.
 *     Deepfake generators often produce unusually uniform
 *     or hyper-smooth intensity distributions.
 *     → Threshold: variance < LOW_VAR or > HIGH_VAR → suspicious
 *
 *  2. BLUR / SMOOTHING DETECTION
 *     Approximates a Laplacian sharpness metric.
 *     AI-generated images frequently have blurred
 *     boundaries and over-smoothed skin/hair regions.
 *     → Threshold: laplacian variance < BLUR_THRESHOLD → suspicious
 *
 *  3. EDGE SHARPNESS INCONSISTENCY
 *     Measures gradient magnitude (Sobel-style) in
 *     different regions of the image and computes the
 *     coefficient of variation across regions.
 *     Real photos have consistent natural noise; deepfakes
 *     have sharp-smooth transitions that betray generation.
 *     → Threshold: CV > EDGE_INCONSISTENCY_THRESHOLD → suspicious
 *
 *  4. FACIAL SYMMETRY APPROXIMATION
 *     Divides the image into left/right halves and
 *     computes mean intensity difference.
 *     Perfect or impossibly broken symmetry is a GAN artifact.
 *     → Threshold: diff < HYPER_SYMMETRIC or diff > ASYM → suspicious
 *
 *  VERDICT: Each rule contributes a weighted score.
 *           Total score ≥ FAKE_THRESHOLD → FAKE
 *           Confidence = (score / maxScore) * 100
 */
@Service
public class ImageAnalysisService {

    // ---- Thresholds (tuned empirically) ----
    private static final double LOW_VARIANCE       = 800.0;
    private static final double HIGH_VARIANCE      = 18000.0;
    private static final double BLUR_THRESHOLD     = 150.0;
    private static final double EDGE_INCONSISTENCY = 0.55;
    private static final double HYPER_SYMMETRY     = 1.5;
    private static final double ASYM_THRESHOLD     = 22.0;

    // ---- Rule weights (sum = 100) ----
    private static final int WEIGHT_VARIANCE   = 25;
    private static final int WEIGHT_BLUR       = 30;
    private static final int WEIGHT_EDGE       = 25;
    private static final int WEIGHT_SYMMETRY   = 20;
    private static final int FAKE_THRESHOLD    = 45;  // score ≥ this → FAKE

    // ---- Sampling grid for performance ----
    private static final int SAMPLE_STEP = 3;  // examine every Nth pixel

    /**
     * Main entry point. Accepts a multipart image file and returns a verdict.
     */
    public AnalysisResult analyze(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("Could not decode image. Ensure file is a valid JPEG, PNG, or WebP.");
        }

        // Downscale very large images for consistent analysis speed
        image = normalizeSize(image, 640);

        int width  = image.getWidth();
        int height = image.getHeight();

        // ---- Extract grayscale pixel matrix ----
        double[][] gray = extractGrayscale(image, width, height);

        // ---- Run rules ----
        List<AnalysisResult.RuleResult> rules = new ArrayList<>();
        int totalScore = 0;

        // Rule 1 — Pixel Intensity Variance
        RuleOutput r1 = checkPixelVariance(gray, width, height);
        rules.add(new AnalysisResult.RuleResult("Pixel Intensity Variance", r1.triggered, r1.score));
        if (r1.triggered) totalScore += WEIGHT_VARIANCE;

        // Rule 2 — Blur / Smoothing Detection
        RuleOutput r2 = checkBlur(gray, width, height);
        rules.add(new AnalysisResult.RuleResult("Blur / Smoothing Detection", r2.triggered, r2.score));
        if (r2.triggered) totalScore += WEIGHT_BLUR;

        // Rule 3 — Edge Sharpness Inconsistency
        RuleOutput r3 = checkEdgeInconsistency(gray, width, height);
        rules.add(new AnalysisResult.RuleResult("Edge Sharpness Inconsistency", r3.triggered, r3.score));
        if (r3.triggered) totalScore += WEIGHT_EDGE;

        // Rule 4 — Facial Symmetry Approximation
        RuleOutput r4 = checkSymmetry(gray, width, height);
        rules.add(new AnalysisResult.RuleResult("Facial Symmetry Analysis", r4.triggered, r4.score));
        if (r4.triggered) totalScore += WEIGHT_SYMMETRY;

        // ---- Verdict ----
        String verdict = (totalScore >= FAKE_THRESHOLD) ? "FAKE" : "REAL";

        // Confidence = how strongly we commit to the verdict
        int maxPossible = WEIGHT_VARIANCE + WEIGHT_BLUR + WEIGHT_EDGE + WEIGHT_SYMMETRY; // 100
        int confidence;
        if ("FAKE".equals(verdict)) {
            // Higher score = more confident it's FAKE
            confidence = 50 + (int)((double) totalScore / maxPossible * 50);
        } else {
            // Lower score = more confident it's REAL
            confidence = 50 + (int)((double)(maxPossible - totalScore) / maxPossible * 50);
        }
        confidence = Math.min(99, Math.max(51, confidence));

        return new AnalysisResult(verdict, confidence, rules);
    }

    // ================================================================
    //  RULE 1 — Pixel Intensity Variance
    // ================================================================
    private RuleOutput checkPixelVariance(double[][] gray, int width, int height) {
        double sum = 0, sumSq = 0;
        int count = 0;

        for (int y = 0; y < height; y += SAMPLE_STEP) {
            for (int x = 0; x < width; x += SAMPLE_STEP) {
                double v = gray[y][x];
                sum   += v;
                sumSq += v * v;
                count++;
            }
        }

        double mean     = sum / count;
        double variance = (sumSq / count) - (mean * mean);

        boolean suspicious = (variance < LOW_VARIANCE) || (variance > HIGH_VARIANCE);

        // Score = how far from the "normal" band [LOW_VAR, HIGH_VAR]
        int score;
        if (variance < LOW_VARIANCE) {
            score = (int) Math.min(100, ((LOW_VARIANCE - variance) / LOW_VARIANCE) * 100);
        } else if (variance > HIGH_VARIANCE) {
            score = (int) Math.min(100, ((variance - HIGH_VARIANCE) / HIGH_VARIANCE) * 60);
        } else {
            score = 0;
        }

        return new RuleOutput(suspicious, score);
    }

    // ================================================================
    //  RULE 2 — Blur Detection (Laplacian Variance)
    // ================================================================
    private RuleOutput checkBlur(double[][] gray, int width, int height) {
        // 3x3 Laplacian kernel:  0  1  0
        //                        1 -4  1
        //                        0  1  0
        double sum = 0, sumSq = 0;
        int count = 0;

        for (int y = 1; y < height - 1; y += SAMPLE_STEP) {
            for (int x = 1; x < width - 1; x += SAMPLE_STEP) {
                double lap = gray[y-1][x] + gray[y+1][x]
                           + gray[y][x-1] + gray[y][x+1]
                           - 4 * gray[y][x];
                sum   += lap;
                sumSq += lap * lap;
                count++;
            }
        }

        double mean     = sum / count;
        double variance = (sumSq / count) - (mean * mean);

        boolean blurry = variance < BLUR_THRESHOLD;
        int score = blurry
            ? (int) Math.min(100, ((BLUR_THRESHOLD - variance) / BLUR_THRESHOLD) * 100)
            : 0;

        return new RuleOutput(blurry, score);
    }

    // ================================================================
    //  RULE 3 — Edge Sharpness Inconsistency (Regional Sobel CV)
    // ================================================================
    private RuleOutput checkEdgeInconsistency(double[][] gray, int width, int height) {
        int gridRows = 4, gridCols = 4;
        double[] regionMag = new double[gridRows * gridCols];
        int idx = 0;

        int cellW = width  / gridCols;
        int cellH = height / gridRows;

        for (int gr = 0; gr < gridRows; gr++) {
            for (int gc = 0; gc < gridCols; gc++) {
                int x0 = gc * cellW + 1;
                int y0 = gr * cellH + 1;
                int x1 = Math.min(x0 + cellW, width  - 1);
                int y1 = Math.min(y0 + cellH, height - 1);

                double magSum = 0;
                int cnt = 0;

                for (int y = y0; y < y1; y += SAMPLE_STEP) {
                    for (int x = x0; x < x1; x += SAMPLE_STEP) {
                        double gx = gray[y][x+1] - gray[y][x-1];
                        double gy = gray[y+1][x] - gray[y-1][x];
                        magSum += Math.sqrt(gx*gx + gy*gy);
                        cnt++;
                    }
                }
                regionMag[idx++] = cnt > 0 ? magSum / cnt : 0;
            }
        }

        // Coefficient of Variation (stdDev / mean) across regions
        double meanMag = 0;
        for (double v : regionMag) meanMag += v;
        meanMag /= regionMag.length;

        if (meanMag == 0) return new RuleOutput(false, 0);

        double varMag = 0;
        for (double v : regionMag) varMag += (v - meanMag) * (v - meanMag);
        varMag /= regionMag.length;
        double cv = Math.sqrt(varMag) / meanMag;

        boolean inconsistent = cv > EDGE_INCONSISTENCY;
        int score = inconsistent
            ? (int) Math.min(100, ((cv - EDGE_INCONSISTENCY) / EDGE_INCONSISTENCY) * 80)
            : 0;

        return new RuleOutput(inconsistent, score);
    }

    // ================================================================
    //  RULE 4 — Facial Symmetry Approximation
    // ================================================================
    private RuleOutput checkSymmetry(double[][] gray, int width, int height) {
        int midX = width / 2;
        double leftSum = 0, rightSum = 0;
        int count = 0;

        for (int y = 0; y < height; y += SAMPLE_STEP) {
            for (int x = 0; x < midX; x += SAMPLE_STEP) {
                int mirrorX = width - 1 - x;
                if (mirrorX < width) {
                    leftSum  += gray[y][x];
                    rightSum += gray[y][mirrorX];
                    count++;
                }
            }
        }

        double leftMean  = leftSum  / count;
        double rightMean = rightSum / count;
        double diff      = Math.abs(leftMean - rightMean);

        // Suspicious: hyper-symmetric (diff < HYPER_SYMMETRY) OR
        //             unnaturally asymmetric (diff > ASYM_THRESHOLD)
        boolean suspicious = (diff < HYPER_SYMMETRY) || (diff > ASYM_THRESHOLD);

        int score;
        if (diff < HYPER_SYMMETRY) {
            score = (int) Math.min(100, ((HYPER_SYMMETRY - diff) / HYPER_SYMMETRY) * 90);
        } else if (diff > ASYM_THRESHOLD) {
            score = (int) Math.min(100, ((diff - ASYM_THRESHOLD) / ASYM_THRESHOLD) * 70);
        } else {
            score = 0;
        }

        return new RuleOutput(suspicious, score);
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    /**
     * Convert BufferedImage to a 2D double array of grayscale values [0, 255].
     */
    private double[][] extractGrayscale(BufferedImage image, int width, int height) {
        double[][] gray = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;
                // Luminosity formula (ITU-R BT.601)
                gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return gray;
    }

    /**
     * Scale image down to maxDim on the longer side if it exceeds that size.
     * Preserves aspect ratio.
     */
    private BufferedImage normalizeSize(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) return src;

        double scale = (double) maxDim / Math.max(w, h);
        int nw = (int)(w * scale);
        int nh = (int)(h * scale);

        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, nw, nh, null);
        g2.dispose();
        return scaled;
    }

    // ---- Simple data holder for a single rule's output ----
    private static class RuleOutput {
        final boolean triggered;
        final int score;
        RuleOutput(boolean triggered, int score) {
            this.triggered = triggered;
            this.score = score;
        }
    }
}
