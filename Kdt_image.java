import bridges.base.*;
import bridges.connect.Bridges;
import bridges.validation.RateLimitException;

import java.io.*;
import java.lang.String;
import java.util.*;


/**
  Use KDTree to process image.
 */

public class Kdt_image {
    // define some constants
    private static Bridges bridges;
    private static int MaxLevel = 12;
    private static float HomogeneityThresh = 2000.0f;
    public static Boolean ShowPartitioners = true; // for viewing/hiding partitioning lines
    public static final Color LINE_COLOR = new Color("white"); // for partition lines
    private static Random random = new Random();

    private static boolean part1, part2, part3, copyMethod = false;

    public Kdt_image() {
        super();
    }

    /**
     * Builds a KD tree representation of a 2D color image.
     * Recursively partitions the image into smaller and smaller regions
     * and tests its pixel colors until they are deemed to be homogeneous (or
     * pass a homogeneity criteria, or the tree reaches a maximum height.
     *
     * @param region {xmin,ymin,xmax,ymax}
     * @param level level of tree
     * @param cg  color-grid to work on
     * @param dim_flag
     * @param draw_partitioners
     * @return  the kdtree
     */
    public static KdTreeElement<Integer, String>
	buildImageTree(int[] region, int level,
		       ColorGrid cg,
		       Boolean dim_flag,
		       Boolean draw_partitioners) {

        // create a kd tree element
        int orientation = (dim_flag) ? 1 : 0;
        KdTreeElement<Integer, String> root = new KdTreeElement<Integer, String>(0, orientation);

        // check the region's homogeneity
        Boolean homogeneous = Image.IsRegionHomogeneous(cg, region, HomogeneityThresh);
 
        if ((level < MaxLevel) && !homogeneous) {
            // partition the region on one of two dimensions
            // here the dimension alternates between X and Y, controlled by
            // a boolean flag
            int partition;

            if (!dim_flag) {  // partition on X (cols)
                // X partition - locate between 1/3 and 2/3
                // of the partition interval
                partition = genRandom(region[0], region[2]);
                root.setPartitioner(partition); // SO TREES CAN BE SAVED AND REUSED

                int[] lregion = {region[0], region[1], partition, region[3]};
                int[] rregion = {partition, region[1], region[2], region[3]};

		        // set children of root to subtrees obtained via recursion
                root.setLeft(buildImageTree(lregion, level + 1, cg, true, draw_partitioners));
                root.setRight(buildImageTree(rregion, level + 1, cg, true, draw_partitioners));

                // color the partition line
                if (draw_partitioners == true) {
                    // find the region of the partitioning line, different for
                    // X or Y partitioned dimension
                    int partitioned_region_x[] = {partition, region[1], partition, region[3]};
                    Image.ColorRegion(cg, partitioned_region_x, LINE_COLOR);
                }


            } else {      // partition on Y (rows)
                // Y partition - locate between 1/3 and 2/3
                // of the partition interval
                partition = genRandom(region[1], region[3]);
                root.setPartitioner(partition);

                // compute the two regions' sub region bounds
                int[] tregion = {region[0], region[1], region[2], partition};
                int[] bregion = {region[0], partition, region[2], region[3]};

		        // set children of root to subtrees obtained via recursion
                root.setLeft(buildImageTree(tregion, level + 1, cg, false, draw_partitioners));
                root.setRight(buildImageTree(bregion, level + 1, cg, false, draw_partitioners));

                // color the partition line
                if (draw_partitioners == true) {
                    int partitioned_region_y[] = {region[0], partition, region[2], partition};
                    Image.ColorRegion(cg, partitioned_region_y, LINE_COLOR);
                }

            }
            return root;
        }

        // BASE: this is a homogeneous region, so color it with average color
        Image.ColorRegion(cg, region);

        return null;
    }

    // FOR SAVED TREES, BASICALLY PASSES OLDTREE TO GET PARTITIONS FROM SINCE THAT IS
    // THE MAIN DIFFERENCE FROM TREE TO TREE. SAME AS PRIOR METHOD BUT FOR SAVED.
    private static KdTreeElement<Integer, String> buildImageTree(KdTreeElement<Integer,
            String> oldTree, int[] region, int level, ColorGrid cg,
                         boolean dim_flag, Boolean draw_partitioners) {

        // create a kd tree element
        int orientation = (dim_flag) ? 1 : 0;
        KdTreeElement<Integer, String> root = oldTree;

        // check the region's homogeneity
        Boolean homogeneous = Image.IsRegionHomogeneous(cg, region, HomogeneityThresh);

        if ((level < MaxLevel) && !homogeneous) {
            // partition the region on one of two dimensions
            // here the dimension alternates between X and Y, controlled by
            // a boolean flag
            int partition;

            if (!dim_flag) {  // partition on X (cols)
                // X partition - locate between 1/3 and 2/3
                // of the partition interval
                partition = root.getPartitioner();

                int[] lregion = {region[0], region[1], partition, region[3]};
                int[] rregion = {partition, region[1], region[2], region[3]};

                // set children of root to subtrees obtained via recursion
                root.setLeft(buildImageTree(oldTree.getLeft(), lregion, level + 1, cg, true, draw_partitioners));
                root.setRight(buildImageTree(oldTree.getRight(), rregion, level + 1, cg, true, draw_partitioners));

                // color the partition line
                if (draw_partitioners == true) {
                    // find the region of the partitioning line, different for
                    // X or Y partitioned dimension
                    int partitioned_region_x[] = {partition, region[1], partition, region[3]};
                    Image.ColorRegion(cg, partitioned_region_x, LINE_COLOR);
                }


            } else {      // partition on Y (rows)
                // Y partition - locate between 1/3 and 2/3
                // of the partition interval
                partition = root.getPartitioner();

                // compute the two regions' sub region bounds
                int[] tregion = {region[0], region[1], region[2], partition};
                int[] bregion = {region[0], partition, region[2], region[3]};

                // set children of root to subtrees obtained via recursion
                root.setLeft(buildImageTree(oldTree.getLeft(), tregion, level + 1, cg, false, draw_partitioners));
                root.setRight(buildImageTree(oldTree.getRight(), bregion, level + 1, cg, false, draw_partitioners));

                // color the partition line
                if (draw_partitioners == true) {
                    int partitioned_region_y[] = {region[0], partition, region[2], partition};
                    Image.ColorRegion(cg, partitioned_region_y, LINE_COLOR);
                }

            }
            return root;
        }

        // BASE: this is a homogeneous region, so color it with average color
        Image.ColorRegion(cg, region);

        return null;
    }

    /**
     *     generate an integer between 1/3 and 2/3 of the min-max range
     */
    public static int genRandom(double min, double max) {
        if (min >= max) {throw new IllegalArgumentException("max must be > min");}

        double onethird = ((max - min) / 3.0) + min;
        double twothird = (((max - min) * 2.0) / 3.0) + min;

        return (int) (random.nextDouble(onethird, twothird));
    }

    public static KdTreeElement<Integer, String> depthChange(Image image, int depth) throws  IOException, RateLimitException {
        MaxLevel = depth;

        ColorGrid cg = null;

        // Convert to ColorGrid
        cg = image.toColorGrid(image);

        bridges.setDataStructure(cg);

        bridges.setTitle("DEPTH OF: " + depth);
        // Call buildImageTree
        KdTreeElement<Integer, String> t = buildImageTree(new int[]{0, 0, cg.getWidth(), cg.getHeight()}, 0, cg, false, ShowPartitioners);

        // Visualize the tree
        bridges.visualize();

        return  t;
    }

    public static void main(String[] args) throws Exception {
        // Bridges credentials
        bridges = new Bridges(0, "hw_",
                "596502733020");

        bridges.setTitle("Image Representation/Compression Using K-D Trees");

        part1 = true; // UNCHECK TO RUN DIFFERENT PARTS
        part2 = false;
        part3 = false;
        copyMethod = true;


        if (part1 == true) {
            Image image = null;

            // Read image
            image = new Image("images/square.ppm");

            ColorGrid cg = null;

            // Convert to ColorGrid
            cg = image.toColorGrid(image);

            bridges.setDataStructure(cg);
            bridges.visualize();

            KdTreeElement<Integer, String> t = buildImageTree(new int[]{0, 0, cg.getWidth(), cg.getHeight()}, 0, cg, false, ShowPartitioners);
            bridges.visualize();


            if (part3 == true) {
                bridges = new Bridges(1, "hw_",
                        "596502733020");

                bridges.setTitle("Image Representation/Compression Using K-D Trees");

                image = null;

                // Read image
                image = new Image("images/square.ppm");

                cg = null;

                // Convert to ColorGrid
                cg = image.toColorGrid(image);

                bridges.setDataStructure(cg);
                bridges.visualize();

                save(t);

                KdTreeElement<Integer, String> k = new KdTreeElement<>();
                k = load("KDTree.bin");

                buildImageTree(k, new int[]{0, 0, cg.getWidth(), cg.getHeight()}, 0, cg, false, ShowPartitioners);
                bridges.visualize();
            }

            if (copyMethod == true) {
                bridges = new Bridges(1, "hw_",
                        "596502733020");

                bridges.setTitle("Image Representation/Compression Using K-D Trees");

                image = null;

                // Read image
                image = new Image("images/square.ppm");

                cg = null;

                // Convert to ColorGrid
                cg = image.toColorGrid(image);

                bridges.setDataStructure(cg);
                bridges.visualize();

                KdTreeElement<Integer, String> k = new KdTreeElement<>();
                k = getCopyOfTree(t);

                buildImageTree(k, new int[]{0, 0, cg.getWidth(), cg.getHeight()}, 0, cg, false, ShowPartitioners);
                bridges.visualize();
            }
        }

        if (part2 == true) {
            Image image = null;

            image = new Image("images/cuomo.ppm");

            ColorGrid cg = null;
            cg = image.toColorGrid(image);

            bridges.setDataStructure(cg);
            bridges.visualize();

            HomogeneityThresh = 1.0f;
            ShowPartitioners = false;
            depthChange(image, 16);
            depthChange(image, 20);
            depthChange(image, 36);
        }
    }

    public static KdTreeElement getCopyOfTree(KdTreeElement<Integer, String> oldTree) {
        KdTreeElement<Integer, String> newTree = new KdTreeElement();
        newTree = (oldTree);
        copy(oldTree, newTree);
        return newTree;
    }

    private static void copy(KdTreeElement<Integer, String> oldEle, KdTreeElement<Integer, String> newEle) {
        if (oldEle.getLeft() != null) {
            newEle.setLeft(oldEle.getLeft());
            copy(oldEle.getLeft(), newEle.getLeft());
        }
        if (oldEle.getRight() != null) {
            newEle.setRight(oldEle.getRight());
            copy(oldEle.getRight(), newEle.getRight());
        }
    }

    public static void save(KdTreeElement t) throws IOException {
        try {
            ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(
                    "KDTree.bin"));

            Integer h = height(t);
            o.writeObject(h);
            for (int i = 1; i <= h; i++) {
                OutputCurrentLevel(t, i, o);
            }

            o.close();

        } catch (FileNotFoundException ex){
            ex.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    static int height(KdTreeElement t) {
        if (t == null) return  0;
        else {
            int lheight = height(t.getLeft());
            int rheight = height(t.getRight());

            if (lheight > rheight) return (lheight + 1);
            else return (rheight + 1);
        }
    }

    private static void OutputCurrentLevel(KdTreeElement t, int level,
                                         ObjectOutputStream o) throws IOException {
        if (t == null) return;

        if (level == 1) o.writeObject(t.getPartitioner());

        else if (level > 1)
            OutputCurrentLevel(t.getLeft(), level - 1, o);
            OutputCurrentLevel(t.getRight(), level - 1, o);
    }

    private static KdTreeElement Construct(ObjectInputStream i, KdTreeElement t, Integer h) throws IOException, ClassNotFoundException {
        if (t.getPartitioner() == null) t.setPartitioner(i.readObject());

        int _h = 0;

        for (int j = 0; j < h; j++) {
            t.setLeft(t);
            t.getLeft().setPartitioner(i.readObject());
            t.setRight(t);
            t.getRight().setPartitioner(i.readObject());
        }

        t.getLeft().setPartitioner(i.readObject());
        t.getRight().setPartitioner(i.readObject());

        return t;
    }


    public static KdTreeElement<Integer, String> load(String filename) {
        KdTreeElement tree = new KdTreeElement<>();
        try {
            ObjectInputStream i = new ObjectInputStream(
                    new FileInputStream("KDTree.bin"));

            Integer h = (Integer) i.readObject();

            Construct(i, tree, h);
            // DEPRECATED

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }
 };
