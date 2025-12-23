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

@WebServlet(urlPatterns = "/item")
public class ItemServlet extends HttpServlet {
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
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);

            String code = jsonObject.get("code").getAsString();
            String description = jsonObject.get("description").getAsString();
            double unitPrice = jsonObject.get("unitPrice").getAsDouble();
            int qtyOnHand = jsonObject.get("qtyOnHand").getAsInt();

            try (Connection connection = ds.getConnection()) {
                String query = "INSERT INTO item (code, description, unit_price, qty_on_hand) VALUES (?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, code);
                preparedStatement.setString(2, description);
                preparedStatement.setDouble(3, unitPrice);
                preparedStatement.setInt(4, qtyOnHand);

                int rowInserted = preparedStatement.executeUpdate();
                if (rowInserted > 0) {
                    resp.getWriter().write("Item Saved Successfully");
                } else {
                    resp.getWriter().write("Item Save Failed");
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);

            String code = jsonObject.get("code").getAsString();
            String description = jsonObject.get("description").getAsString();
            double unitPrice = jsonObject.get("unitPrice").getAsDouble();
            int qtyOnHand = jsonObject.get("qtyOnHand").getAsInt();

            try (Connection connection = ds.getConnection()) {
                String query = "UPDATE item SET description=?, unit_price=?, qty_on_hand=? WHERE code=?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, description);
                preparedStatement.setDouble(2, unitPrice);
                preparedStatement.setInt(3, qtyOnHand);
                preparedStatement.setString(4, code);

                int rowUpdated = preparedStatement.executeUpdate();
                if (rowUpdated > 0) {
                    resp.getWriter().write("Item Updated Successfully");
                } else {
                    resp.getWriter().write("Item Update Failed");
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection connection = ds.getConnection()) {
            String query = "SELECT * FROM item ORDER BY code";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            JsonArray itemList = new JsonArray();

            while (resultSet.next()) {
                String code = resultSet.getString("code");
                String description = resultSet.getString("description");
                double unitPrice = resultSet.getDouble("unit_price");
                int qtyOnHand = resultSet.getInt("qty_on_hand");

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("code", code);
                jsonObject.addProperty("description", description);
                jsonObject.addProperty("unitPrice", unitPrice);
                jsonObject.addProperty("qtyOnHand", qtyOnHand);
                itemList.add(jsonObject);
            }

            Gson gson = new Gson();
            resp.getWriter().write(gson.toJson(itemList));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            resp.getWriter().write(error.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        String code = req.getParameter("code");

        try (Connection connection = ds.getConnection()) {
            String query = "DELETE FROM item WHERE code=?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, code);

            int rowDeleted = preparedStatement.executeUpdate();
            if (rowDeleted > 0) {
                resp.getWriter().write("Item Deleted Successfully");
            } else {
                resp.getWriter().write("Item Delete Failed");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}