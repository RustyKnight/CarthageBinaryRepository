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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.Part;

/**
 *
 * @author shanewhitehead
 */
public enum RepositoryManager {
    INSTANCE;

    private final static Logger LOGGER = Logger.getLogger(RepositoryManager.class.getName());

    protected static final String REPOSITORY_LOCATION_KEY = "Repository.location";

    private RepositoryManager() {
    }

    protected String getRepositoryRoot(ServletContext context) {
        return context.getInitParameter(REPOSITORY_LOCATION_KEY);
    }
    
    protected File rootPath(ServletContext context) {
        String root = getRepositoryRoot(context);
        if (root == null) {
            return null;
        }
        File path = new File(root);
        return path;
    }
    
    protected File path(ServletContext context, String library) {
        return new File(rootPath(context), library);
    }
    
    protected File path(ServletContext context, String library, String tag) {
        return new File(path(context, library), tag);
    }
    
    protected File path(ServletContext context, String library, String tag, String version) {
        return new File(path(context, library, tag), version);
    }
    
    public List<String> getLibraries(ServletContext context) {
        ArrayList<String> libraries = new ArrayList<>();

        File path = rootPath(context);
        LOGGER.log(Level.INFO, "List libraries from " + path);
        if (!path.exists() || !path.isDirectory()) {
            return libraries;
        }

        File[] libs = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        LOGGER.log(Level.INFO, "Found " + libs.length + " possible libraries");
        for (File lib : libs) {
            libraries.add(lib.getName());
        }

        Collections.sort(libraries);
        
        return libraries;
    }

    public List<String> getTags(ServletContext context, String library) {
        ArrayList<String> tags = new ArrayList<>();

        File filePath = path(context, library);
        if (filePath == null) {
            LOGGER.log(Level.INFO, "No repository path configured");
            return tags;
        }
        if (!filePath.exists() || !filePath.isDirectory()) {
            LOGGER.log(Level.INFO, filePath + " either does not exist or is not a directory");
            return tags;
        }
        File[] avaliableTags = filePath.listFiles((File pathname) -> {
//            String name1 = pathname.getName();
//            return name1.matches("^[0-9]+.[0-9]+.[0-9]+$");
            return pathname.isDirectory();
        });

        for (File tag : avaliableTags) {
            tags.add(tag.getName());
        }
        
        Collections.sort(tags);

//        Collections.sort(tags, new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                return o1.compareTo(o2);
//            }
//        });

        return tags;
    }

    public List<Version> getVersions(ServletContext context, String library, String tag) {
        ArrayList<Version> versions = new ArrayList<>();

        File filePath = path(context, library, tag);
        if (filePath == null) {
            LOGGER.log(Level.INFO, "No repository path configured");
            return versions;
        }
        if (!filePath.exists() || !filePath.isDirectory()) {
            LOGGER.log(Level.INFO, filePath + " either does not exist or is not a directory");
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
                LOGGER.log(Level.INFO, version.getName() + " is an invalid directory");
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

    public List<Binary> getBinariesAvaliableByVersion(ServletContext context, String library, String tag, String version) {
        File root = rootPath(context);
        if (root == null) {
            LOGGER.log(Level.INFO, "No repository path configured");
            return new ArrayList<>();
        }

        ArrayList<Binary> binaries = new ArrayList<>(25);
        try {
            Version libraryVersion = new Version(version);
            
            File path = path(context, library, tag, version);
            
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
        
        Collections.sort(binaries, new Comparator<Binary>() {
            @Override
            public int compare(Binary lhs, Binary rhs) {
                return lhs.getBinary().getName().compareTo(rhs.getBinary().getName());
            }
        });
        
        return binaries;
    }

    public List<Binary> getXcodeBinaries(ServletContext context, String library, String tag, String xCodeVersion) {
        File filePath = path(context, library, tag);
        if (filePath == null) {
            LOGGER.log(Level.INFO, "No repository path configured");
            return new ArrayList<>();
        }

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

    public File getBinary(ServletContext context, String library, String tag, String version, String xcodeVersion) {
        File filePath = path(context, library, tag, version);
        if (filePath == null) {
            LOGGER.log(Level.INFO, "No repository path configured");
            return null;
        }
        
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

        LOGGER.log(Level.INFO, "Requesting binary for " + library + "/" + tag + "/" + version + "/" + xcodeVersion + " sending " + files[0].getPath());

        return files[0];
    }
//		String name = request.getParameter("name");
//		String libraryVersion = request.getParameter("version");
//		String xcodeVersion = request.getParameter("xcodeVersion");
//		String xcodeBuild = request.getParameter("xcodeBuild");
//		Part filePart = request.getPart("binary");
//		
//		RepositoryManager.INSTANCE.upload(getServletContext(), 
//						name, libraryVersion, xcodeVersion, xcodeBuild,
//						filePart	);

    public void upload(ServletContext context, String library, String tag, String libraryVersion, String xcodeVersion, String xcodeBuild, Part filePart) throws InvalidParameterException, IOException {
        File filePath = path(context, library, tag, libraryVersion);
        if (filePath == null) {
            throw new IOException("Repository store is not configured");
        }
        Version version = new Version(libraryVersion);
        System.out.println("Store path = " + filePath);
        LOGGER.log(Level.INFO, "Store path = " + filePath);
        if (!(filePath.exists() || filePath.mkdirs())) {
            System.out.println("Failed to create output path");
            throw new IOException("Could not create required output path");
        }

        String fileName = xcodeVersion + "b" + xcodeBuild + ".zip";
        System.out.println("fileName = " + fileName);
        LOGGER.log(Level.INFO, "Library name = " + fileName);

        File libraryFile = new File(filePath, fileName);
        LOGGER.log(Level.INFO, "Library file = " + libraryFile);
        try ( InputStream is = filePart.getInputStream();  OutputStream os = new FileOutputStream(libraryFile)) {
            byte bytes[] = new byte[4096];
            int bytesRead = -1;
            long totalBytesRead = 0;
            while ((bytesRead = is.read(bytes)) != -1) {
                totalBytesRead += bytesRead;
                os.write(bytes, 0, bytesRead);
            }
            System.out.println("totalBytesRead = " + totalBytesRead);
            LOGGER.log(Level.INFO, "totalBytesRead = " + totalBytesRead);
        }

        if (!libraryFile.exists()) {
            System.out.println("!! Didn't seem to write file");
            LOGGER.log(Level.INFO, "!! Didn't seem to write file");
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
