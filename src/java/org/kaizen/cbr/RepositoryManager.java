/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kaizen.cbr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.tomcat.util.codec.binary.Base64;

/**
 *
 * @author shanewhitehead
 */
public enum RepositoryManager {
    INSTANCE;

    protected static final String REPOSITORY_LOCATION_KEY = "Repository.location";

    private RepositoryManager() {
    }

    protected String getRepositoryRoot(ServletContext context) {
        return context.getInitParameter(REPOSITORY_LOCATION_KEY);
    }

    public List<Version> getVersions(ServletContext context, String library) {
        ArrayList<Version> versions = new ArrayList<>();
        String path = getRepositoryRoot(context);
        if (path == null) {
            System.out.println("No repository path configured");
            return versions;
        }
        File filePath = new File(path, library);
        if (!filePath.exists() || !filePath.isDirectory()) {
            System.out.println(filePath + " either does not exist or is not a directory");
            return versions;
        }
        File[] avaliableVersions = filePath.listFiles((File pathname) -> {
            String name1 = pathname.getName();
            return name1.matches("^[0-9]+.[0-9]+.[0-9]+$");
        });

        for (File version : avaliableVersions) {
            try {
                versions.add(new Version(version.getName()));
            } catch (InvalidParameterException exp) {
                System.out.println(version.getName() + " is an invalid directory");
            }
        }

        Collections.sort(versions, new Comparator<Version>() {
            @Override
            public int compare(Version o1, Version o2) {
                return o1.compareTo(o2);
            }
        });

        return versions;
    }

    public List<Binary> getBinariesAvaliableByVersion(ServletContext context, String library, String version) {
        String root = getRepositoryRoot(context);
        if (root == null) {
            System.out.println("No repository path configured");
            return new ArrayList<>();
        }

        ArrayList<Binary> binaries = new ArrayList<>(25);
        try {
            Version libraryVersion = new Version(version);
            File path = new File(new File(root, library), version);
            File[] releases = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase().endsWith(".zip");
                }
            });
            for (File file : releases) {
                binaries.add(new Binary(libraryVersion, file));                
            }
        } catch (InvalidParameterException ex) {
            return new ArrayList<>();
        }
        return binaries;
    }

    public List<Binary> getXcodeBinaries(ServletContext context, String library, String xCodeVersion) {
        String path = getRepositoryRoot(context);
        if (path == null) {
            System.out.println("No repository path configured");
            return new ArrayList<>();
        }
        File filePath = new File(path, library);

        return findReleases(filePath, xCodeVersion);
    }

    protected List<Binary> findReleases(File root, String xCodeVersion) {
        List<Binary> binaries = new ArrayList<Binary>();
        File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    Version version = new Version(file.getName());
                    binaries.addAll(findBinaries(file, xCodeVersion, version));
                } catch (InvalidParameterException ex) {
                }
            }
        }
        return binaries;
    }

    protected List<Binary> findBinaries(File root, String xCodeVersion, Version version) {
        List<Binary> binaries = new ArrayList<Binary>();
        File[] files = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".zip");
            }
        });
        // We'd be lucky ... or stupid ... to find more then one
        for (File file : files) {
            if (file.getName().contains(xCodeVersion)) {
                binaries.add(new Binary(version, file));
            }
        }
        return binaries;
    }

    public File getBinary(ServletContext context, String library, String version, String xcodeVersion) {
        String path = getRepositoryRoot(context);
        if (path == null) {
            System.out.println("No repository path configured");
            return null;
        }
        File filePath = new File(new File(path, library), version);
        File[] files = filePath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName().toLowerCase();
                return name.endsWith(".zip") && name.contains(xcodeVersion.toLowerCase());
            }
        });
        if (files.length == 0) {
            return null;
        }

        System.out.println("Requesting binary for " + library + "/" + version + "/" + xcodeVersion + " sending " + files[0].getPath());

        return files[0];
    }
    
    public void upload(ServletContext context, Upload upload) throws InvalidParameterException, IOException {
        String root = getRepositoryRoot(context);
        if (root == null) {
            throw new IOException("Repository store is not configured");
        }
        Version version = new Version(upload.libraryVersion);
        String path = root + "/" + upload.name + "/" + version.toString();
        System.out.println("Store path = " + path);
        File filePath = new File(path);
        if (!(filePath.exists() || filePath.mkdirs())) {
            throw new IOException("Could not create required output path");
        }
        
        String name = upload.xcodeVersion + "b" + upload.xcodeBuild + ".zip";
        System.out.println("Library name = " + name);
        
        File libraryFile = new File(filePath, name);
        
        byte[] data = Base64.decodeBase64(upload.data);
        try (OutputStream os = new FileOutputStream(libraryFile)) {
            os.write(data);
        }
        
        if (!libraryFile.exists()) {
            System.out.println("!! Didn't seem to write file");
            throw new IOException("Failed to store library in repository");
        }
    }

    public class Binary {

        private Version vesion;
        private File binary;

        public Binary(Version vesion, File binary) {
            this.vesion = vesion;
            this.binary = binary;
        }

        public Version getVesion() {
            return vesion;
        }

        public File getBinary() {
            return binary;
        }

    }

    public static class InvalidParameterException extends Exception {

        public InvalidParameterException(String message) {
            super(message);
        }

        public InvalidParameterException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static class Version implements Comparable<Version> {
        
        public static boolean isVersion(String text) {
            try {
                Version version = new Version(text);
            } catch (InvalidParameterException ex) {
                return false;
            }
            return true;
        }

        private int major;
        private int minor;
        private int patch;

        public Version(String version) throws InvalidParameterException {
            String[] parts = version.split("\\.");
            if (parts.length != 3) {
                throw new InvalidParameterException("Version value must be in {major}.{minor}.{patch} format");
            }
            try {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
                patch = Integer.parseInt(parts[2]);
            } catch (NumberFormatException exp) {
                throw new InvalidParameterException("Version value must be in {major}.{minor}.{patch} format", exp);
            }
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }

        @Override
        public String toString() {
            return getMajor() + "." + getMinor() + "." + getPatch();
        }

        @Override
        public int compareTo(Version o) {
            if (getMajor() != o.getMajor()) {
                return getMajor() - o.getMajor();
            }
            if (getMinor() != o.getMinor()) {
                return getMinor() - o.getMinor();
            }
            if (getPatch() != o.getPatch()) {
                return getPatch() - o.getPatch();
            }
            return 0;
        }

    }
}
