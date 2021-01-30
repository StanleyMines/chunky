/* Copyright (c) 2017-2021 Jesper Öqvist <jesper@llbit.se>
 * Copyright (c) 2017-2021 Chunky contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.renderer;

import org.junit.Test;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.projection.ProjectionMode;
import se.llbit.chunky.renderer.scene.SampleBuffer;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.Sky;
import se.llbit.json.JsonObject;
import se.llbit.math.Vector3;
import se.llbit.math.Vector4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Simple integration tests to verify that rendering
 * a blank scene works as it should.
 * The tests render using a small canvas size and with
 * only two samples per pixel.
 */
public class TestBlankRender {
  private static final int WIDTH = Math.max(10, Scene.MIN_CANVAS_WIDTH);
  private static final int HEIGHT = Math.max(10, Scene.MIN_CANVAS_HEIGHT);

  /**
   * Renders one sample per pixel and checks that the color values are
   * close enough to the expected color.
   */
  private static void renderAndCheckSamples(Scene scene, double[] expected)
        throws InterruptedException {
    SampleBuffer samples = render(scene);
    long valueCount = samples.numberOfDoubles();
    for (int y = 0; y < HEIGHT; y++)
      for (int x = 0; x < WIDTH; x++)
        // Check each channel value:
        for (int c = 0; c < 3; ++c)
          if (samples.get(x, y, c) < expected[c]-0.005
                || samples.get(x, y, c) > expected[c]+0.005)
          {
            assertEquals("Sampled pixel is outside expected value range.",
                  expected[c], samples.get(x, y, c), 0.005);
            fail("Sampled pixel is outside expected value range.");
          }
  }

  /** Renders a scene and returns the resulting sample buffer. */
  private static SampleBuffer render(Scene scene) throws InterruptedException {
    // A single worker thread is used, with fixed PRNG seed.
    // This makes the path tracing results deterministic.
    ChunkyOptions options = ChunkyOptions.getDefaults();
    options.renderThreads = 1;
    Chunky chunky = new Chunky(options);
    RenderContext context = new RenderContext(chunky);
    context.workerFactory =
          (renderManager, index, seed) -> new RenderWorker(renderManager, index, 0);
    RenderManager renderer = new RenderManager(context, true);
    renderer.setSceneProvider(new MockSceneProvider(scene));
    renderer.start();
    renderer.join();
    return renderer.getBufferedScene().getSampleBuffer();
  }

  /** Compares two sample buffers. */
  private static void compareSamples(SampleBuffer expected, SampleBuffer actual, double delta)
        throws InterruptedException {
    long valueCount = expected.numberOfDoubles();
    double act, exp;
    for (long i = 0; i < valueCount; ++i) {
      act = actual.get(i);
      exp = expected.get(i);
      if (act < exp - delta || act > exp + delta) {
        assertEquals("Sampled pixel is outside expected value range.",
            exp, act, delta);
        fail("Sampled pixel is outside expected value range.");
      }
    }
  }

  /**
   * Render with a fully black sky.
   */
  @Test public void testBlackSky() throws InterruptedException {
    final Scene scene = new Scene();
    scene.setCanvasSize(WIDTH, HEIGHT);
    scene.setRenderMode(RenderMode.RENDERING);
    scene.setTargetSpp(2);
    scene.setName("foobar");
    scene.sky().setSkyMode(Sky.SkyMode.BLACK);
    renderAndCheckSamples(scene, new double[] {0, 0, 0});
  }

  /**
   * Render with a solid sky color.
   */
  @Test public void testSolidColorSky() throws InterruptedException {
    final Scene scene = new Scene();
    scene.setCanvasSize(WIDTH, HEIGHT);
    scene.setRenderMode(RenderMode.RENDERING);
    scene.setTargetSpp(2);
    scene.setName("foobar");
    scene.sky().setSkyMode(Sky.SkyMode.SOLID_COLOR);
    scene.sky().setColor(new Vector3(0.9, 0.8, 1.0));
    renderAndCheckSamples(scene, new double[] { 0.9, 0.8, 1.0 });
  }
  /**
   * Render with a gray gradient sky.
   */
  @Test public void testGradientSky() throws InterruptedException {
    final Scene scene = new Scene();
    scene.setCanvasSize(WIDTH, HEIGHT);
    scene.setRenderMode(RenderMode.RENDERING);
    scene.sky().setSkyMode(Sky.SkyMode.GRADIENT);
    List<Vector4> white = new ArrayList<>();
    white.add(new Vector4(0.5, 0.5, 0.5, 0));
    white.add(new Vector4(0.5, 0.5, 0.5, 1));
    scene.sky().setGradient(white);
    scene.setTargetSpp(2);
    scene.setName("gray");
    renderAndCheckSamples(scene, new double[] {0.5, 0.5, 0.5});
  }

  /**
   * Test that render output is correct after JSON export/import.
   */
  @Test public void testJsonRoundTrip1() throws InterruptedException {
    final Scene scene = new Scene();
    scene.setCanvasSize(WIDTH, HEIGHT);
    scene.setRenderMode(RenderMode.RENDERING);
    scene.sky().setSkyMode(Sky.SkyMode.GRADIENT);
    List<Vector4> white = new ArrayList<>();
    white.add(new Vector4(0.5, 1, 0.25, 0));
    white.add(new Vector4(0.5, 1, 0.25, 1));
    scene.sky().setGradient(white);
    scene.setTargetSpp(2);
    scene.setName("json1");
    JsonObject json = scene.toJson();
    scene.fromJson(json);
    scene.setRenderMode(RenderMode.RENDERING); // Un-pause after JSON import.
    renderAndCheckSamples(scene, new double[] {0.5, 1, 0.25});
  }

  /**
   * Test that render output is correct after JSON export/import.
   */
  @Test public void testJsonRoundTrip2() throws InterruptedException {
    final Scene scene = new Scene();
    scene.setTargetSpp(2);
    scene.setName("json2");
    scene.setCanvasSize(WIDTH, HEIGHT);
    scene.setRenderMode(RenderMode.RENDERING);
    scene.sky().setSkyMode(Sky.SkyMode.SIMULATED);
    scene.camera().setProjectionMode(ProjectionMode.PANORAMIC);
    scene.camera().setFoV(100);

    SampleBuffer samples1 = new SampleBuffer(render(scene));

    JsonObject json = scene.toJson();
    scene.fromJson(json);
    scene.setRenderMode(RenderMode.RENDERING); // Un-pause after JSON import.

    compareSamples(samples1, render(scene), 0.005);
  }
}
