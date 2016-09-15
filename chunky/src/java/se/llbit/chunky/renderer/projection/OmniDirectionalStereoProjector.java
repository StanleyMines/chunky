/* Copyright (c) 2016 Chunky contributors
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
package se.llbit.chunky.renderer.projection;

import org.apache.commons.math3.util.FastMath;
import se.llbit.math.Vector3;

import java.util.Random;

/**
 * A projector for Omni-Directional Stereo (ODS) images. x is mapped to yaw, y is mapped to
 * pitch. This projector is like the {@link PanoramicProjector} but can create distinct
 * images for the left and the right eye to create panoramic stereo images that are perfect
 * for viewing on VR devices.
 *
 * @see <a href="https://developers.google.com/vr/jump/rendering-ods-content.pdf">Rendering Omni‐directional Stereo Content</a>
 */
public class OmniDirectionalStereoProjector implements Projector {
  /**
  * The eye that this projector produces rays for.
  */
  private final Eye eye;

  /**
   * The interpupillary distance of the viewer, in meters.
   */
  private double interpupillaryDistance = 0.069;

  public OmniDirectionalStereoProjector(Eye eye) {
    this.eye = eye;
  }

  @Override
  public void apply(double x, double y, Random random, Vector3 pos, Vector3 direction) {
    apply(x, y, pos, direction);
  }

  @Override
  public void apply(double x, double y, Vector3 pos, Vector3 direction) {
    double theta = (x + 0.5) * FastMath.PI - FastMath.PI;
    double phi = FastMath.PI / 2 - (y + 0.5) * FastMath.PI;

    double scale;
    if (eye == Eye.LEFT) {
      scale = -interpupillaryDistance / 2;
    } else {
      scale = interpupillaryDistance / 2;
    }
    pos.set(FastMath.cos(theta) * scale, 0, FastMath.sin(theta) * scale);
    direction.set(FastMath.sin(theta) * FastMath.cos(phi), FastMath.sin(phi), -FastMath.cos(theta) * FastMath.cos(phi));
  }

  @Override
  public double getMinRecommendedFoV() {
    return 180;
  }

  @Override
  public double getMaxRecommendedFoV() {
    return 180;
  }

  @Override
  public double getDefaultFoV() {
    return 180;
  }

  public enum Eye {
    LEFT,
    RIGHT
  }
}
