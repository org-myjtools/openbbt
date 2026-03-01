package org.myjtools.openbbt.tui.mock;

import org.myjtools.openbbt.tui.model.PlanNode;
import org.myjtools.openbbt.tui.model.PlanNode.Type;

public class MockData {

    public static PlanNode createMockPlan() {
        var project = new PlanNode("project", "My Project", Type.PROJECT);

        var feature1 = new PlanNode("f1", "User Authentication", Type.FEATURE);
        feature1.addChild(scenario("sc1", "Successful login",
            step("Given I have valid credentials"),
            step("When I submit the login form"),
            step("Then I am redirected to the dashboard")));
        feature1.addChild(scenario("sc2", "Failed login with wrong password",
            step("Given I have invalid credentials"),
            step("When I submit the login form"),
            step("Then I see an error message")));
        feature1.addChild(scenario("sc3", "Password reset flow",
            step("Given I am on the login page"),
            step("When I click 'Forgot password'"),
            step("And I enter my email address"),
            step("Then I receive a password reset email")));

        var feature2 = new PlanNode("f2", "Shopping Cart", Type.FEATURE);
        feature2.addChild(scenario("sc4", "Add item to cart",
            step("Given I am on a product page"),
            step("When I click 'Add to cart'"),
            step("Then the cart count increases by 1")));
        feature2.addChild(scenario("sc5", "Remove item from cart",
            step("Given I have an item in my cart"),
            step("When I click 'Remove'"),
            step("Then the item is no longer in my cart")));
        feature2.addChild(scenario("sc6", "Checkout process",
            step("Given I have items in my cart"),
            step("When I proceed to checkout"),
            step("And I fill in my payment details"),
            step("Then my order is placed successfully")));

        var feature3 = new PlanNode("f3", "REST API", Type.FEATURE);
        feature3.addChild(scenario("sc7", "GET /users returns 200",
            step("Given the API is available"),
            step("When I make a GET request to /users"),
            step("Then the HTTP status code is 200"),
            step("And the response body contains a list of users")));
        feature3.addChild(scenario("sc8", "POST /users creates a user",
            step("Given the API is available"),
            step("When I POST to /users with valid payload"),
            step("Then the HTTP status code is 201"),
            step("And the response body contains the created user")));

        project.addChild(feature1);
        project.addChild(feature2);
        project.addChild(feature3);
        return project;
    }

    private static PlanNode scenario(String id, String label, PlanNode... steps) {
        var node = new PlanNode(id, label, Type.SCENARIO);
        for (var step : steps) node.addChild(step);
        return node;
    }

    private static PlanNode step(String label) {
        return new PlanNode(label, label, Type.STEP);
    }
}