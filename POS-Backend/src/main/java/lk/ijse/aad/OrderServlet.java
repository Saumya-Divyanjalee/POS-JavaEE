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

@WebServlet(urlPatterns = "/order")
public class OrderServlet extends HttpServlet {
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
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // CORS headers
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        Connection connection = null;

        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);

            String orderId = jsonObject.get("orderId").getAsString();
            String orderDate = jsonObject.get("orderDate").getAsString();
            String customerId = jsonObject.get("customerId").getAsString();
            JsonArray orderDetails = jsonObject.getAsJsonArray("orderDetails");

            // Start transaction
            connection = ds.getConnection();
            connection.setAutoCommit(false);

            try {
                // 1. Insert into orders table
                String orderQuery = "INSERT INTO orders (order_id, order_date, customer_id) VALUES (?, ?, ?)";
                PreparedStatement orderStmt = connection.prepareStatement(orderQuery);
                orderStmt.setString(1, orderId);
                orderStmt.setString(2, orderDate);
                orderStmt.setString(3, customerId);

                int orderInserted = orderStmt.executeUpdate();
                if (orderInserted <= 0) {
                    throw new SQLException("Failed to insert order");
                }

                // 2. Insert order details and update item quantities
                String detailQuery = "INSERT INTO order_details (order_id, item_code, qty, unit_price) VALUES (?, ?, ?, ?)";
                String updateItemQuery = "UPDATE item SET qty_on_hand = qty_on_hand - ? WHERE code = ?";
                String checkQtyQuery = "SELECT qty_on_hand FROM item WHERE code = ?";

                PreparedStatement detailStmt = connection.prepareStatement(detailQuery);
                PreparedStatement updateItemStmt = connection.prepareStatement(updateItemQuery);
                PreparedStatement checkQtyStmt = connection.prepareStatement(checkQtyQuery);

                for (int i = 0; i < orderDetails.size(); i++) {
                    JsonObject detail = orderDetails.get(i).getAsJsonObject();
                    String itemCode = detail.get("itemCode").getAsString();
                    int qty = detail.get("qty").getAsInt();
                    double unitPrice = detail.get("unitPrice").getAsDouble();

                    // Check if enough quantity available
                    checkQtyStmt.setString(1, itemCode);
                    ResultSet rs = checkQtyStmt.executeQuery();

                    if (rs.next()) {
                        int availableQty = rs.getInt("qty_on_hand");
                        if (availableQty < qty) {
                            throw new SQLException("Insufficient quantity for item: " + itemCode +
                                    ". Available: " + availableQty + ", Required: " + qty);
                        }
                    } else {
                        throw new SQLException("Item not found: " + itemCode);
                    }

                    // Insert order detail
                    detailStmt.setString(1, orderId);
                    detailStmt.setString(2, itemCode);
                    detailStmt.setInt(3, qty);
                    detailStmt.setDouble(4, unitPrice);

                    int detailInserted = detailStmt.executeUpdate();
                    if (detailInserted <= 0) {
                        throw new SQLException("Failed to insert order detail for item: " + itemCode);
                    }

                    // Update item quantity
                    updateItemStmt.setInt(1, qty);
                    updateItemStmt.setString(2, itemCode);

                    int itemUpdated = updateItemStmt.executeUpdate();
                    if (itemUpdated <= 0) {
                        throw new SQLException("Failed to update quantity for item: " + itemCode);
                    }
                }

                // Commit transaction
                connection.commit();
                resp.getWriter().write("Order placed successfully: " + orderId);

            } catch (SQLException e) {
                // Rollback on error
                if (connection != null) {
                    try {
                        connection.rollback();
                        System.err.println("Transaction rolled back due to: " + e.getMessage());
                    } catch (SQLException rollbackEx) {
                        System.err.println("Error during rollback: " + rollbackEx.getMessage());
                    }
                }
                throw e;
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // CORS headers
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String orderId = req.getParameter("orderId");

        try (Connection connection = ds.getConnection()) {

            if (orderId != null && !orderId.isEmpty()) {
                // Get specific order with details
                getOrderById(orderId, connection, resp);
            } else {
                // Get all orders
                getAllOrders(connection, resp);
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            resp.getWriter().write(error.toString());
            e.printStackTrace();
        }
    }

    private void getAllOrders(Connection connection, HttpServletResponse resp) throws SQLException, IOException {
        String query = "SELECT o.order_id, o.order_date, o.customer_id, c.name as customer_name, " +
                "SUM(od.qty * od.unit_price) as total " +
                "FROM orders o " +
                "JOIN customer c ON o.customer_id = c.id " +
                "LEFT JOIN order_details od ON o.order_id = od.order_id " +
                "GROUP BY o.order_id, o.order_date, o.customer_id, c.name " +
                "ORDER BY o.order_date DESC";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        JsonArray orderList = new JsonArray();

        while (resultSet.next()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("orderId", resultSet.getString("order_id"));
            jsonObject.addProperty("orderDate", resultSet.getString("order_date"));
            jsonObject.addProperty("customerId", resultSet.getString("customer_id"));
            jsonObject.addProperty("customerName", resultSet.getString("customer_name"));
            jsonObject.addProperty("total", resultSet.getDouble("total"));
            orderList.add(jsonObject);
        }

        Gson gson = new Gson();
        resp.getWriter().write(gson.toJson(orderList));
    }

    private void getOrderById(String orderId, Connection connection, HttpServletResponse resp)
            throws SQLException, IOException {

        // Get order header
        String orderQuery = "SELECT o.order_id, o.order_date, o.customer_id, c.name as customer_name, " +
                "c.address, c.phone, c.email " +
                "FROM orders o " +
                "JOIN customer c ON o.customer_id = c.id " +
                "WHERE o.order_id = ?";

        PreparedStatement orderStmt = connection.prepareStatement(orderQuery);
        orderStmt.setString(1, orderId);
        ResultSet orderRs = orderStmt.executeQuery();

        if (!orderRs.next()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Order not found");
            resp.getWriter().write(error.toString());
            return;
        }

        JsonObject order = new JsonObject();
        order.addProperty("orderId", orderRs.getString("order_id"));
        order.addProperty("orderDate", orderRs.getString("order_date"));
        order.addProperty("customerId", orderRs.getString("customer_id"));
        order.addProperty("customerName", orderRs.getString("customer_name"));
        order.addProperty("customerAddress", orderRs.getString("address"));
        order.addProperty("customerPhone", orderRs.getString("phone"));
        order.addProperty("customerEmail", orderRs.getString("email"));

        // Get order details
        String detailQuery = "SELECT od.item_code, i.description, od.qty, od.unit_price, " +
                "(od.qty * od.unit_price) as total " +
                "FROM order_details od " +
                "JOIN item i ON od.item_code = i.code " +
                "WHERE od.order_id = ?";

        PreparedStatement detailStmt = connection.prepareStatement(detailQuery);
        detailStmt.setString(1, orderId);
        ResultSet detailRs = detailStmt.executeQuery();

        JsonArray details = new JsonArray();
        double grandTotal = 0;

        while (detailRs.next()) {
            JsonObject detail = new JsonObject();
            detail.addProperty("itemCode", detailRs.getString("item_code"));
            detail.addProperty("description", detailRs.getString("description"));
            detail.addProperty("qty", detailRs.getInt("qty"));
            detail.addProperty("unitPrice", detailRs.getDouble("unit_price"));
            detail.addProperty("total", detailRs.getDouble("total"));
            details.add(detail);
            grandTotal += detailRs.getDouble("total");
        }

        order.add("orderDetails", details);
        order.addProperty("grandTotal", grandTotal);

        Gson gson = new Gson();
        resp.getWriter().write(gson.toJson(order));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // CORS headers
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        String orderId = req.getParameter("orderId");

        if (orderId == null || orderId.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Order ID is required");
            return;
        }

        Connection connection = null;

        try {
            connection = ds.getConnection();
            connection.setAutoCommit(false);

            try {
                // 1. Get order details to restore item quantities
                String getDetailsQuery = "SELECT item_code, qty FROM order_details WHERE order_id = ?";
                PreparedStatement getDetailsStmt = connection.prepareStatement(getDetailsQuery);
                getDetailsStmt.setString(1, orderId);
                ResultSet rs = getDetailsStmt.executeQuery();

                // 2. Restore item quantities
                String updateItemQuery = "UPDATE item SET qty_on_hand = qty_on_hand + ? WHERE code = ?";
                PreparedStatement updateItemStmt = connection.prepareStatement(updateItemQuery);

                while (rs.next()) {
                    String itemCode = rs.getString("item_code");
                    int qty = rs.getInt("qty");

                    updateItemStmt.setInt(1, qty);
                    updateItemStmt.setString(2, itemCode);
                    updateItemStmt.executeUpdate();
                }

                // 3. Delete order details
                String deleteDetailsQuery = "DELETE FROM order_details WHERE order_id = ?";
                PreparedStatement deleteDetailsStmt = connection.prepareStatement(deleteDetailsQuery);
                deleteDetailsStmt.setString(1, orderId);
                deleteDetailsStmt.executeUpdate();

                // 4. Delete order
                String deleteOrderQuery = "DELETE FROM orders WHERE order_id = ?";
                PreparedStatement deleteOrderStmt = connection.prepareStatement(deleteOrderQuery);
                deleteOrderStmt.setString(1, orderId);

                int rowDeleted = deleteOrderStmt.executeUpdate();

                if (rowDeleted > 0) {
                    connection.commit();
                    resp.getWriter().write("Order deleted successfully");
                } else {
                    throw new SQLException("Order not found");
                }

            } catch (SQLException e) {
                if (connection != null) {
                    connection.rollback();
                }
                throw e;
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}