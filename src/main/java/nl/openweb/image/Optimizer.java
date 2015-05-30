package nl.openweb.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

public class Optimizer {

    private File srcDirectory;
    private File targetDirectory;
    private ImageFilter imageFilter = new ImageFilter();
    private DirectoryFilter directoryFilter = new DirectoryFilter();

    public Optimizer(File srcDirectory, File targetDirectory) {
        validateFolder(srcDirectory);
        validateFolder(targetDirectory);
        this.srcDirectory = srcDirectory;
        this.targetDirectory = targetDirectory;
    }

    private void validateFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("the given argument is not a folder.");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            new Optimizer(new File(args[0]), new File(args[1])).optimize();
        } else {
            System.out.println("Please provide the following arguments: [Source folder] [Target folder]");
        }

    }

    private void optimize() throws IOException {
        optimizeDirectoryRecursively(srcDirectory, targetDirectory);
    }

    private void optimizeDirectoryRecursively(File src, File target) throws IOException {
        File[] images = src.listFiles(imageFilter);
        for (File image : images) {
            optimizeImage(target, image);
        }
        File[] subfolders = src.listFiles(directoryFilter);
        for (File subfolder : subfolders) {
            File subfolderTarget = new File(target.getAbsolutePath() + File.separator + subfolder.getName());
            subfolderTarget.mkdir();
            optimizeDirectoryRecursively(subfolder, subfolderTarget);
        }
    }

    private void optimizeImage(File target, File image) {
        try {
            String extension = extractFileExtension(image.getName());
            if ("png".equals(extension)) {
                optimizePngImage(target, image);
            } else {
                optimizeNonePngImage(target, image);
            }
        } catch (Exception e) {
            System.err.println("An exception was thrown while processing \"" + image.getAbsolutePath() + "\"");
            e.printStackTrace();
        }
    }

    private void optimizeNonePngImage(File target, File image) throws IOException {
        ImageWriter imageWriter = null;
        try {
            BufferedImage originalImage = ImageIO.read(image);
            imageWriter = getImageWriter(image);
            ImageWriteParam writeParam = imageWriter.getDefaultWriteParam();

            if (writeParam.canWriteCompressed()) {
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionType(compressionTypes[0]);
                    writeParam.setCompressionQuality(0.85F);
                }
            }

            if (writeParam.canWriteProgressive()) {
                writeParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            }

            FileImageOutputStream output = new FileImageOutputStream(new File(target.getAbsolutePath() + File.separator
                    + image.getName()));
            imageWriter.setOutput(output);
            IIOImage outimage = new IIOImage(originalImage, null, null);
            imageWriter.write(null, outimage, writeParam);
        } finally {
            if (imageWriter != null) {
                imageWriter.dispose();
            }
        }
    }

    private void optimizePngImage(File target, File image) throws IOException {
        PngOptimizer optimizer = new PngOptimizer("error");
        PngImage pngImage = new PngImage(image.getAbsolutePath());
        optimizer.optimize(pngImage, target.getAbsolutePath() + File.separator + image.getName(), true, 9);
    }

    private ImageWriter getImageWriter(File image) {
        String extension = extractFileExtension(image.getName());
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersBySuffix(extension);
        return imageWriters.next();
    }

    private static String extractFileExtension(String name) {
        String extension = null;
        if (name != null && name.lastIndexOf('.') > 0) {
            extension = name.substring(name.lastIndexOf('.') + 1);
        }
        return extension;
    }

    private static class DirectoryFilter implements FileFilter {
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    private static class ImageFilter implements FilenameFilter {
        private final Set<String> imageExtensionsSet = new HashSet<String>();

        public ImageFilter() {
            for (String extension : ImageIO.getWriterFileSuffixes()) {
                imageExtensionsSet.add(extension);
            }
        }

        public boolean accept(File dir, String name) {
            String extension = extractFileExtension(name);
            return imageExtensionsSet.contains(extension);
        }
    }

}
