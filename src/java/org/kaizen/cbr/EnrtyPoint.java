/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kaizen.cbr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.kaizen.cbr.RepositoryManager.Binary;
import org.kaizen.cbr.RepositoryManager.Version;

/**
 *
 * @author shanewhitehead
 */
@MultipartConfig
public class EnrtyPoint extends HttpServlet {

	private final static Logger LOGGER = Logger.getLogger(EnrtyPoint.class.getName());

//	/**
//	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
//	 * methods.
//	 *
//	 * @param request servlet request
//	 * @param response servlet response
//	 * @throws ServletException if a servlet-specific error occurs
//	 * @throws IOException if an I/O error occurs
//	 */
//	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
//					throws ServletException, IOException {
//		response.setContentType("text/html;charset=UTF-8");
//
//		try (PrintWriter out = response.getWriter()) {
//			/* TODO output your page here. You may use following sample code. */
//			out.println("<!DOCTYPE html>");
//			out.println("<html>");
//			out.println("<head>");
//			out.println("<title>Servlet EnrtyPoint</title>");
//			out.println("</head>");
//			out.println("<body>");
//			out.println("<h1>Servlet EnrtyPoint at " + request.getContextPath() + "</h1>");
//			out.println("</body>");
//			out.println("</html>");
//		}
//	}
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

			List<String> libraries = RepositoryManager.INSTANCE.getLibraries(getServletContext());
			out.println("<ul>");
			for (String lib : libraries) {
				out.println("<li><a href='" + lib + "'>" + lib + "</a></li>");
			}
			out.println("</ul>");

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
				out.println("<li><a href='" + library + "/" + version.toString() + "'>" + version.toString() + "</a></li>");

				out.println("<ul>");
				List<Binary> binaries = RepositoryManager.INSTANCE.getBinariesAvaliableByVersion(getServletContext(), library, version.toString());
				for (Binary binary : binaries) {
					String name = binary.getBinary().getName();
					String anchor = library + "/" + name.replace(".zip", "");
					out.println("<li><a href='" + anchor+ "'>" + name + "</a></li>");
				}
				out.println("</ul>");
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
			out.println("{");
			String path = "https://" + request.getServerName() + ":" + request.getServerPort() + "/binary/";
			for (Binary binary : binaries) {
				String name = library + "-v" + binary.getVesion() + "-Xcode" + xcodeVersion + "-framework.zip";
				
				out.println("\t\"" + binary.getVesion() + "\": \"" + 
								path + 
								library + "/" + binary.getVesion() + "/" + xcodeVersion + "/" + name + "\"");
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
		response.sendError(404, "Unknown resource");
	}

	protected void downloadBinary(List<String> parts, HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
		
		if (parts.size() != 5) {
			response.sendError(404, "Unknown resource");
			return;
		}
		
//		binary/KeychainAccess/3.1.1/10.0b10A255/KeychainAccess-v3.1.1-Xcode10.0b10A255-framework.zip

		String library = parts.get(1);
		String version = parts.get(2);
		String xcode = parts.get(3);
		
		String name = parts.get(4);

		LOGGER.log(Level.INFO, "library = " + library);
		LOGGER.log(Level.INFO, "version = " + version);
		LOGGER.log(Level.INFO, "xcode = " + xcode);
		LOGGER.log(Level.INFO, "name = " + name);
		
		File file = RepositoryManager.INSTANCE.getBinary(getServletContext(), library, version, xcode);
		if (file == null) {
			LOGGER.log(Level.SEVERE, "No library exists");
			response.sendError(404, "Unknown resource");
			return;
		}

		LOGGER.log(Level.INFO, "Libary stored @ " + file);
		
		response.setContentType("application/zip");
		response.setHeader("Content-disposition", "attachment; filename=" + name);
		response.setHeader("Content-Length", Long.toString(file.length()));
		try (InputStream is = new FileInputStream(file);
						OutputStream os = response.getOutputStream()) {
			byte[] bytes = new byte[4096];
			int bytesRead = -1;
			long totalBytesRead = 0;
			while ((bytesRead = is.read(bytes)) != -1) {
				totalBytesRead += bytesRead;
				os.write(bytes, 0, bytesRead);
			}
			LOGGER.log(Level.INFO, "totalBytesRead = " + totalBytesRead);
		}
		
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
		
		if (parts.size() > 1 && parts.get(0).equals("binary")) {
			downloadBinary(parts, request, response);
		} else if (parts.size() == 0) {
			// Do we want to list the libaries?
			LOGGER.log(Level.INFO, "List avaliable libraries");
			processLibraryList(request, response);
		} else if (parts.size() == 1) {
			LOGGER.log(Level.INFO, "List avaliable library versions");
			processVersionList(parts.get(0), request, response);
		} else if (parts.size() == 2) {
			String text = parts.get(1);
			if (Version.isVersion(text)) {
				LOGGER.log(Level.INFO, "List avaliable library binary versions");
				processVersionBinariesList(parts.get(0), parts.get(1), request, response);
			} else {
				LOGGER.log(Level.INFO, "List avaliable xcode binary versions");
				processXcodeBinariesList(parts.get(0), parts.get(1), request, response);
			}
		} else if (parts.size() == 3) {
			LOGGER.log(Level.INFO, "Get binary");
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
		LOGGER.log(Level.INFO, "DoPost");

//		request.getSession().setMaxInactiveInterval(6000000);
		String name = request.getParameter("name");
		String libraryVersion = request.getParameter("version");
		String xcodeVersion = request.getParameter("xcodeVersion");
		String xcodeBuild = request.getParameter("xcodeBuild");
		Part filePart = request.getPart("binary");

		LOGGER.log(Level.INFO, "name = " + name);
		LOGGER.log(Level.INFO, "libraryVersion = " + libraryVersion);
		LOGGER.log(Level.INFO, "xcodeVersion = " + xcodeVersion);
		LOGGER.log(Level.INFO, "xcodeBuild = " + xcodeBuild);

		try {
			LOGGER.log(Level.INFO, "xcodeBuild = " + xcodeBuild);
			RepositoryManager.INSTANCE.upload(getServletContext(),
							name, libraryVersion, xcodeVersion, xcodeBuild,
							filePart);
		} catch (RepositoryManager.InvalidParameterException exp) {
			LOGGER.log(Level.SEVERE, "Failed to uplod", exp);
			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().println("");
		}
//		File tempFile = File.createTempFile("upload", ".dat");
//		try (FileOutputStream fos = new FileOutputStream(tempFile);
//						InputStream is = request.getInputStream()) {
//			byte[] bytes = new byte[4096];
//			int bytesRead = -1;
//			long totalBytes = 0;
//			while ((bytesRead = is.read(bytes)) != -1) {
//				totalBytes += bytesRead;
//				fos.write(bytes, 0, bytesRead);
//			}
//			LOGGER.log(Level.INFO, "totalBytes = " + totalBytes);
//			try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
//				LOGGER.log(Level.INFO, "Load JSON");
//				Gson gson = new Gson();
//				Upload upload = gson.fromJson(br, Upload.class);
////			Upload upload = gson.fromJson(sb.toString(), Upload.class);
//				LOGGER.log(Level.INFO, "Storing upload");
//				RepositoryManager.INSTANCE.upload(getServletContext(), upload);
//
//			} catch (RepositoryManager.InvalidParameterException ex) {
//				LOGGER.log(Level.SEVERE, "Failed to store upload", ex);
//				throw new ServletException("Failed to store upload", ex);
//			} catch (Exception ex) {
//				LOGGER.log(Level.SEVERE, "Failed to store upload", ex);
//				throw new ServletException("Failed to store upload", ex);
//			} catch (Error error) {
//				LOGGER.log(Level.SEVERE, "Failed to store upload", error);
//			}
//		}
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException ex) {
//		}
//		LOGGER.log(Level.INFO, "Sending response");
//		response.setStatus(200);
//		response.setContentType("text/plain");
//		response.getOutputStream().println("All good, thanks");
//		response.flushBuffer();

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
