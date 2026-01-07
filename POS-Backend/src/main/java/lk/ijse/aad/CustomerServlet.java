package lk.ijse.aad;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(urlPatterns = "/customer")
public class CustomerServlet extends HttpServlet {
    BasicDataSource ds;

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        ds = (BasicDataSource) servletContext.getAttribute("dataSource");
        if (ds == null) {
            throw new ServletException("DataSource not found in ServletContext");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);

            String id = jsonObject.get("cid").getAsString();
            String name = jsonObject.get("cname").getAsString();
            String address = jsonObject.get("caddress").getAsString();
            String phone = jsonObject.get("cphone").getAsString();
            String email = jsonObject.get("cemail").getAsString();

            try (Connection connection = ds.getConnection()) {
                String query = "INSERT INTO customer (id, name, address, phone, email) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, address);
                preparedStatement.setString(4, phone);
                preparedStatement.setString(5, email);

                int rowInserted = preparedStatement.executeUpdate();
                if (rowInserted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "success");
                    response.addProperty("message", "Customer Saved Successfully");
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "error");
                    response.addProperty("message", "Customer Save Failed");
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Database Error: " + e.getMessage());
            resp.getWriter().write(error.toString());
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Invalid request: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);

            String id = jsonObject.get("cid").getAsString();
            String name = jsonObject.get("cname").getAsString();
            String address = jsonObject.get("caddress").getAsString();
            String phone = jsonObject.get("cphone").getAsString();
            String email = jsonObject.get("cemail").getAsString();

            try (Connection connection = ds.getConnection()) {
                String query = "UPDATE customer SET name=?, address=?, phone=?, email=? WHERE id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, address);
                preparedStatement.setString(3, phone);
                preparedStatement.setString(4, email);
                preparedStatement.setString(5, id);

                int rowUpdated = preparedStatement.executeUpdate();
                if (rowUpdated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "success");
                    response.addProperty("message", "Customer Updated Successfully");
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "error");
                    response.addProperty("message", "Customer not found or Update Failed");
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Database Error: " + e.getMessage());
            resp.getWriter().write(error.toString());
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Invalid request: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection connection = ds.getConnection()) {
            String query = "SELECT * FROM customer ORDER BY id";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            JsonArray customerList = new JsonArray();

            while (resultSet.next()) {
                String cId = resultSet.getString("id");
                String cName = resultSet.getString("name");
                String cAddress = resultSet.getString("address");
                String cPhone = resultSet.getString("phone");
                String cEmail = resultSet.getString("email");

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("cid", cId);
                jsonObject.addProperty("cname", cName);
                jsonObject.addProperty("caddress", cAddress);
                jsonObject.addProperty("cphone", cPhone);
                jsonObject.addProperty("cemail", cEmail);
                customerList.add(jsonObject);
            }

            Gson gson = new Gson();
            resp.getWriter().write(gson.toJson(customerList));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Database Error: " + e.getMessage());
            resp.getWriter().write(error.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String id = req.getParameter("cid");

            if (id == null || id.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("status", "error");
                error.addProperty("message", "Customer ID is required");
                resp.getWriter().write(error.toString());
                return;
            }

            try (Connection connection = ds.getConnection()) {
                // First check if customer exists
                String checkQuery = "SELECT id FROM customer WHERE id=?";
                PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                checkStmt.setString(1, id);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("status", "error");
                    error.addProperty("message", "Customer not found with ID: " + id);
                    resp.getWriter().write(error.toString());
                    return;
                }

                // Delete the customer
                String query = "DELETE FROM customer WHERE id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, id);

                int rowDeleted = preparedStatement.executeUpdate();

                Gson gson = new Gson();
                if (rowDeleted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "success");
                    response.addProperty("message", "Customer Deleted Successfully");
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    JsonObject error = new JsonObject();
                    error.addProperty("status", "error");
                    error.addProperty("message", "Customer Delete Failed");
                    resp.getWriter().write(gson.toJson(error));
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");

            // Check for foreign key constraint violation
            if (e.getMessage().contains("foreign key constraint")) {
                error.addProperty("message", "Cannot delete customer - customer has related orders");
            } else {
                error.addProperty("message", "Database Error: " + e.getMessage());
            }

            resp.getWriter().write(error.toString());
            e.printStackTrace();
        }
    }
}