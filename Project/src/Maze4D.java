import java.util.Random;
import java.util.HashSet;
import java.io.IOException;
import java.io.FileOutputStream;

public class Maze4D {
    public static int size, max;
    public static final int planes = 4;

    public static Point4D[] points;
    public static HashSet<Point4D> maze;

    public static void main(String[] args) throws IOException {
        size = 5; // size of each dimension/plane
        Point4D p, q;
        Random rand = new Random();
        max = (int)Math.pow(size,planes); // Number of 4D points in the maze
        boolean printOutput = false, writeOutput = false;

        long t1 = System.nanoTime();
        initializeMaze(); // Create points
        long t2 = System.nanoTime();

        while(maze.size() != 1) {
            int i = rand.nextInt(max); // Pick a random point
            p = points[i];

            // Pick a random wall to knock down
            int plane, dir;
            do {
                plane = rand.nextInt(planes);
                dir = rand.nextInt(2); dir = dir == 0 ? -1 : 1;
                q = (Point4D)p.clone();
                q.update(plane,dir);
            } while(!q.isWithinBounds(size));

            // Get the point on the other side of the wall that we're about to knock down
            i = q.coords[3] + size*q.coords[2] + size*size*q.coords[1] + size*size*size*q.coords[0];
            q = points[i];

            int pwall, qwall;
            switch(plane) {
                case 0: // t-axis wall will be knocked down
                    pwall = dir == -1 ? -128 : -64;
                    qwall = dir == -1 ? -64 : -128;
                    break;
                case 1: // z-axis wall will be knocked down
                    pwall = dir == -1 ? -32 : -16;
                    qwall = dir == -1 ? -16 : -32;
                    break;
                case 2: // y-axis wall will be knocked down
                    pwall = dir == -1 ? -8 : -4;
                    qwall = dir == -1 ? -4 : -8;
                    break;
                case 3: // x-axis wall will be knocked down
                    pwall = dir == -1 ? -2 : -1;
                    qwall = dir == -1 ? -1 : -2;
                    break;
                default: pwall = 0; qwall = 0; break;
            }
            union(p, q, pwall, qwall); // Break down the wall and combine the sets of points
        }
        long t3 = System.nanoTime();

        double initTime     = ((double)t2-t1) / 1000000000;
        double computeTime  = ((double)t3-t2) / 1000000000;
        System.out.printf("Init time: %.4f s%nRun time: %.4f s%n",initTime,computeTime);

        long t4 = System.nanoTime();
        if(printOutput) {
            for (Point4D point : points)
                System.out.println(String.format("%8s", Integer.toBinaryString(point.walls & 0xFF)).replace(' ', '0'));
        } else if(writeOutput) {
            byte[] ans = new byte[max];
            for (int i = 0; i < max; i++) ans[i] = points[i].walls;

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream("maze.txt");
                fos.write(ans);
                fos.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }

        if(printOutput || writeOutput) {
            double writeTime = ((double) System.nanoTime() - t4) / 1000000000;
            System.out.printf("Write time: %.4f s%n", writeTime);
        }


    }

    public static void initializeMaze() {
        maze = new HashSet<>();
        points = new Point4D[max];

        for(int t = 0; t < size; t++) {
            for (int z = 0; z < size; z++) {
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int i = x + y*size + z*size*size + t*size*size*size;
                        Point4D p = new Point4D(t, z, y, x);

                        maze.add(p);
                        points[i] = p;
                    }
                }
            }
        }
    }

    public static Point4D find(Point4D point) {
        if(point.parent == point) return point;

        Point4D parent = find(point.parent);
        point.parent = parent; // Path compression
        return parent;
    }

    public static void union(Point4D p, Point4D q, int pWall, int qWall) {
        Point4D proot = find(p), qroot = find(q);
        if(proot == qroot) return; // No need to join because p & q aren't disjoint

        // If root ranks are the same, increase one root's rank by 1
        if(proot.rank == qroot.rank) ++proot.rank;

        // Make lower rank child of higher rank root
        if(proot.rank > qroot.rank) {
            maze.remove(qroot.parent);
            qroot.parent = proot;
        }
        else {
            maze.remove(proot.parent);
            proot.parent = qroot;
        }

        // Update walls
        p.walls += pWall;
        q.walls += qWall;
    }
}

class Point4D {
    public int rank;
    public int[] coords;
    public Point4D parent;
    public byte walls;

    public Point4D(int t, int z, int y, int x) {
        this.coords = new int[] {t, z, y, x};
        this.rank = 0;
        this.parent = this;
        this.walls = (byte) -1;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof Point4D)) return false;
        Point4D point = (Point4D)o;

        return this.coords[0] == point.coords[0] && this.coords[1] == point.coords[1]
                && this.coords[2] == point.coords[2] && this.coords[3] == point.coords[3];
    }

    public void update(int plane, int distance) {
        this.coords[plane] += distance;
    }

    public boolean isWithinBounds(int n) {
        boolean validT = this.coords[0] >= 0 && this.coords[0] < n;
        boolean validZ = this.coords[1] >= 0 && this.coords[1] < n;
        boolean validY = this.coords[2] >= 0 && this.coords[2] < n;
        boolean validX = this.coords[3] >= 0 && this.coords[3] < n;
        return validX && validY && validZ && validT;
    }

    @Override
    public Object clone() {
        return new Point4D(this.coords[0], this.coords[1], this.coords[2], this.coords[3]);
    }

    @Override
    public String toString() {
        return "(" + (this.coords[0] + "," + this.coords[1] + this.coords[2] + "," + this.coords[3]) + ")";
    }
}