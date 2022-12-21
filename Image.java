import java.io.*;
import java.text.FieldPosition;
import java.util.Scanner;

import bridges.base.ColorGrid;
import bridges.base.Color;


/**
 * Read and store image.
 * Enables conversion to Bridges ColorGrid for display.
 * @author HENRY WANDOVER
 */
public class Image {
    // If a default color is needed, use this one.
    private static final Color DEFAULT_COLOR = new Color("black");
    private int width, height, maxVal; // image dimensions
    private static int[] image_array; // image array to store


    public Image() {
        width = height = maxVal = 0;
        image_array = null;
    }

    /**
     * creates an image object by reading the input image in
     * binary PPM
     *  All IO exceptions are thrown and must be
     *  dealt with by caller.
     *
     * @param input_file name of PPM file (binary,not ascii)
     */

    public Image(String input_file) throws IOException, FileNotFoundException {
        File f = new File(input_file); // Creates File obj
        FileInputStream fileInputStream = new FileInputStream(f); // Creates FileInputStream

        // Read header of 4 lines using readLine
        readLine(fileInputStream); // Reads line with magic number "P6"
        readLine(fileInputStream); // GIMP attribution

        Scanner sc = new Scanner(readLine(fileInputStream)); // Use Scanner to extract numbers for lines 3 and 4
        width = sc.nextInt(); // 3rd line has width and height
        height = sc.nextInt();

        sc = new Scanner(readLine(fileInputStream));
        maxVal = sc.nextInt(); // 4th line has maxVal
        sc.close();


        image_array = new int[height * width * 3];

        // Read in data as bytes.
        byte[] bytes = new byte[image_array.length];

        for (int i = 0; i < image_array.length; i++) {
            bytes[i] = (byte) (fileInputStream.read());
        }

        // Convert to this.image_array using Byte.toUnsignedInt()
        for (int i = 0; i < image_array.length; i++) {
            image_array[i] = Byte.toUnsignedInt(bytes[i]);
        }
        fileInputStream.close();
    }

   /*
       Read a maximum of n bytes, until newline is found.
       @return string up to first newline.
   */
    private String readLine(FileInputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        char c;
        while (true) {
            c = (char) stream.read();
            if (c != '\n') sb.append(c);
            else break;
        }
        return sb.toString();
    }


    /**
     * Color-grid tutorial
     * http://bridgesuncc.github.io/tutorials/ColorGrid.html
     *
     * @return img-array as color-grid
     */
    public ColorGrid toColorGrid(Image image) {
        int[] _image_array = image_array;

        // Create ColorGrid of correct dimensions.
        // Fill it with data from img-array.
	
        ColorGrid cg = new ColorGrid(height, width);

        int c = 0;

        int num_squares_x = width;
        int num_squares_y = height;

        int sq_width = width / num_squares_x,
                sq_height = width / num_squares_x;

        for (int i = 0; i < num_squares_y; i++) {
            for (int j = 0; j < num_squares_x; j++) {
                Color color;

                color = new Color(_image_array[c], _image_array[c+1],
                        _image_array[c+2]);

                int origin_x = j * sq_width;
                int origin_y = i * sq_height;

                for (int row = origin_y; row < origin_y + sq_height; row++) {
                    for (int column = origin_x; column < origin_x + sq_width; column++) {
                        cg.set(row, column, color);
                    }
                }
                c += 3;
            }
        }

        return cg;
    }

    /**
     * Return avg RGB in ColorGrid for entire region
     * @param cg color-grid to process
     * @param region rgb triplets
     * @return avg RGB
     */
    public static Color avgColor(ColorGrid cg, int[] region) {
	    int[] rgb = {0,0,0}; // red, green, and blue
        int size = regionSize(region);

        for (int row = region[1]; row < region[3]; row++) {
            for (int col = region[0]; col < region[2]; col++) {
                Color pix = cg.get(row, col);
                rgb[0] += pix.getRed();
                rgb[1] += pix.getGreen();
                rgb[2] += pix.getBlue();
            }
        }

        rgb[0] = rgb[0] / (size);
        rgb[1] = rgb[1] / (size);
        rgb[2] = rgb[2] / (size);

        return new Color(rgb[0],rgb[1],rgb[2]);
    }

    /**
     *  Color the region with an average color
     * @param cg  grid to set
     * @param region used to set grid.
     */
    public static void ColorRegion(ColorGrid cg, int[] region) {
        ColorRegion(cg,region,avgColor(cg,region));
    }


    /**
     Color the provided region with the provided color.
     Used for partitioning lines; all pixels have the same color
     as the region is homogeneous.
     There are two versions of this function, depending on whether a
     constant or average color is used.  The average color is the average
     r,g,b of all pixels in the region.
     Use function overloading to implement the functions
     *
     * @param cg  the color-grid
     * @param region the region to use
     * @param c the color
     */
    public static void ColorRegion(ColorGrid cg, int[] region, Color c) {
        if (Kdt_image.ShowPartitioners && c == Kdt_image.LINE_COLOR) {
            if (region[0] == region[2]) {
                int yMIN = region[1];
                int yMAX = region[3];

                do {
                    cg.set(yMIN, region[0], c);
                    yMIN++;
                } while (yMIN != yMAX);

            } else if (region[1] == region[3]) {
                int xMIN = region[0];
                int xMAX = region[2];

                do {
                    cg.set(region[1], xMIN, c);
                    xMIN++;
                } while (xMIN != xMAX);
            }
        } else {
            for (int row = region[1]; row < region[3]; row++) {
                for (int col = region[0]; col < region[2]; col++) {
                    cg.set(row, col, c);
                }
            }
        }
    }


    /**
     * Test a given region for homogeneity, i.e.,
     * if the region is within a threshold for approximation
     *
     * @param cg color-grid
     * @param region
     * @return true iff homogenous
     */
    public static Boolean IsRegionHomogeneous(ColorGrid cg, int[] region, double thresh) {
        int size = regionSize(region);

        // Minimum is 4 pixels (return true if < 4)
        if (size < 4) return true;

        Color avg = avgColor(cg, region);
        double[] RGBav = {avg.getRed(), avg.getGreen(), avg.getBlue()};
        double VAR = 0.0;

        // need to compute variance here for RGB (each color separately)
        for (int row = region[1]; row < region[3]; row++) {
            for (int col = region[0]; col < region[2]; col++) {
                Color pix = cg.get(row, col);
                double varR = (pix.getRed() - RGBav[0]);
                double R = (varR * varR) / size;
                double varG = (pix.getGreen() - RGBav[1]);
                double G = (varG * varG) / size;
                double varB = (pix.getBlue() - RGBav[2]);
                double B = (varB * varB) / size;

                VAR += R;
                VAR += G;
                VAR += B;
            }
        }

	    // If variance of the sum of the three is less than some small
	    // value, return true, else false.  ????
        if (VAR < thresh) return true;
        else return false;
    }

    public static int regionSize(int region[]) {
        int totalX = region[2] - region[0];
        int totalY = region[3] - region[1];

        if (totalX == 0) totalX = 1;
        if (totalY == 0) totalY = 1;

        return totalX * totalY;
    }
}

