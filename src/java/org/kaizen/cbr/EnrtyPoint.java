/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kaizen.cbr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.kaizen.cbr.RepositoryManager.Binary;
import org.kaizen.cbr.RepositoryManager.Version;

/**
 *
 * @author shanewhitehead
 */
public class EnrtyPoint extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        System.out.println("contextPath = " + request.getContextPath());
        System.out.println("queryString = " + request.getQueryString());
        System.out.println("requestURI = " + request.getRequestURI());
        System.out.println("requestURL = " + request.getRequestURL());

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet EnrtyPoint</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet EnrtyPoint at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    protected void processLibraryList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>List of awesome libraries</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>List of awesome libraries</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    protected void processVersionList(String library, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<RepositoryManager.Version> versions = RepositoryManager.INSTANCE.getVersions(getServletContext(), library);
        if (versions.size() == 0) {
            response.sendError(404, "No releases avaliable for " + library);
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>List avaliable versions for library</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Avaliable releases for [" + library + "]</h1>");
            out.println("<ul>");
            for (Version version : versions) {
                out.println("<li>" + version.toString() + "</li>");
            }
            out.println("</ul>");
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    protected void processVersionBinariesList(String library, String version, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<Binary> binaries = RepositoryManager.INSTANCE.getBinariesAvaliableByVersion(getServletContext(), library, version);
        if (binaries.size() == 0) {
            response.sendError(404, "No binaries avaliable for " + library + "/v-" + version);
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>List avaliable versions for library</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Avaliable Xcode binary releases for [" + library + "/v" + version + "]</h1>");
            out.println("<ul>");
            for (Binary binary : binaries) {
                String name = binary.getBinary().getName();
                name = name.replace("Xcode-", "");
                name = name.replace("Xcode", "");
                name = name.replace("-framework.zip", "");
                name = name.replace(".framework.zip", "");
                name = name.replace("framework.zip", "");
                out.println("<li>" + name + "</li>");
            }
            out.println("</ul>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    protected void processXcodeBinariesList(String library, String xcodeVersion, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<Binary> binaries = RepositoryManager.INSTANCE.getXcodeBinaries(getServletContext(), library, xcodeVersion);
        if (binaries.size() == 0) {
            response.sendError(404, "No binaries avaliable for " + library + "/Xcode-" + xcodeVersion);
            return;
        }

        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("{");
            String path = "https://" + request.getServerName() + ":" + request.getServerPort() + "/";
            for (Binary binary : binaries) {
                // At this point, the name is irrelevent, it only matters that 
                // it is carried as part of the overall query
                out.println("\t\"" + binary.getVesion() + "\": \"" + path + library + "/" + binary.getVesion() + "/" + xcodeVersion + "-framework.zip\"");
            }
            out.println("}");
        }
    }

    protected void processBinary(String library, String version, String xcodeVersion, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        xcodeVersion = xcodeVersion.replace("-framework.zip", "");

        File file = RepositoryManager.INSTANCE.getBinary(getServletContext(), library, version, xcodeVersion);
        if (file == null) {
            response.sendError(404, "No binary avaliable for " + library + "/" + version + "-" + xcodeVersion);
        }

        response.setContentType("application/zip");
        response.setHeader("Content-disposition", "attachment; filename=" + library + "-v" + version + "-Xcode-" + xcodeVersion + "-framework.zip");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    protected void processBadRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("Perfoming post :)");
        response.sendError(404, "Unknown resource");
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
        String path = request.getRequestURI();
        List<String> parts = new ArrayList<String>(Arrays.asList(path.split("\\/")));
        if (parts.size() > 0) {
            parts.remove(0);
        }
        if (parts.size() == 0) {
            // Do we want to list the libaries?
            System.out.println("List libraries");
            processLibraryList(request, response);
        } else if (parts.size() == 1) {
            System.out.println("List library versions");
            processVersionList(parts.get(0), request, response);
        } else if (parts.size() == 2) {
            String text = parts.get(1);
            if (Version.isVersion(text)) {
                System.out.println("List library, binaries avaliable for version");
                processVersionBinariesList(parts.get(0), parts.get(1), request, response);
            } else {
                System.out.println("List library, binaries avaliable for xcode");
                processXcodeBinariesList(parts.get(0), parts.get(1), request, response);
            }
        } else if (parts.size() == 3) {
            System.out.println("Get binary");
            processBinary(parts.get(0), parts.get(1), parts.get(2), request, response);
        } else {
            processBadRequest(request, response);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("DoPost");
//        processRequest(request, response);

        StringBuilder sb = new StringBuilder(256);
        try (BufferedReader br = request.getReader()) {
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        
        System.out.println(sb.toString());
        
        response.setContentType("text/plain");
        response.setStatus(200);
        response.getOutputStream().println("All good, thanks");

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Carthage Binary Repository";
    }// </editor-fold>

}
