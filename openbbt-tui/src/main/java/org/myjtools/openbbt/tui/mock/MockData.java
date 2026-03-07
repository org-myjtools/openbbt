package org.myjtools.openbbt.tui.mock;

import org.myjtools.openbbt.tui.model.PlanNode;
import org.myjtools.openbbt.tui.model.PlanNode.Type;
import org.myjtools.openbbt.tui.model.PlanNode.Status;

public class MockData {

    public static PlanNode createMockPlan() {
        var project = new PlanNode("project", "My Project", Type.PROJECT);

        // Feature 1: has one invalid step
        var feature1 = new PlanNode("f1", "User Authentication", Type.FEATURE);
        feature1.addChild(scenario("sc1", "Successful login",
            ok("Given I have valid credentials"),
            ok("When I submit the login form"),
            ok("Then I am redirected to the dashboard")));
        feature1.addChild(scenarioWithIssues("sc2", "Failed login with wrong password",
            ok("Given I have invalid credentials"),
            invalid("When I submit the login form", "Step not found: 'When I submit the login form'"),
            ok("Then I see an error message")));
        feature1.addChild(scenario("sc3", "Password reset flow",
            ok("Given I am on the login page"),
            ok("When I click 'Forgot password'"),
            ok("And I enter my email address"),
            ok("Then I receive a password reset email")));
        feature1.setStatus(Status.HAS_ISSUES);

        // Feature 2: fully valid
        var feature2 = new PlanNode("f2", "Shopping Cart", Type.FEATURE);
        feature2.addChild(scenario("sc4", "Add item to cart",
            ok("Given I am on a product page"),
            ok("When I click 'Add to cart'"),
            ok("Then the cart count increases by 1")));
        feature2.addChild(scenario("sc5", "Remove item from cart",
            ok("Given I have an item in my cart"),
            ok("When I click 'Remove'"),
            ok("Then the item is no longer in my cart")));
        feature2.addChild(scenario("sc6", "Checkout process",
            ok("Given I have items in my cart"),
            ok("When I proceed to checkout"),
            ok("And I fill in my payment details"),
            ok("Then my order is placed successfully")));
        feature2.setStatus(Status.VALIDATED);

        // Feature 3: two invalid steps
        var feature3 = new PlanNode("f3", "REST API", Type.FEATURE);
        feature3.addChild(scenarioWithIssues("sc7", "GET /users returns 200",
            ok("Given the API is available"),
            invalid("When I make a GET request to /users", "No REST step provider found"),
            ok("Then the HTTP status code is 200"),
            ok("And the response body contains a list of users")));
        feature3.addChild(scenarioWithIssues("sc8", "POST /users creates a user",
            ok("Given the API is available"),
            ok("When I POST to /users with valid payload"),
            invalid("Then the HTTP status code is 201", "Assertion step not bound: 'Then the HTTP status code is 201'"),
            ok("And the response body contains the created user")));
        feature3.setStatus(Status.HAS_ISSUES);

        project.addChild(feature1);
        project.addChild(feature2);
        project.addChild(feature3);
        project.setStatus(Status.HAS_ISSUES);
        return project;
    }

    private static PlanNode scenario(String id, String label, PlanNode... steps) {
        var node = new PlanNode(id, label, Type.SCENARIO);
        for (var step : steps) node.addChild(step);
        node.setStatus(Status.VALIDATED);
        return node;
    }

    private static PlanNode scenarioWithIssues(String id, String label, PlanNode... steps) {
        var node = new PlanNode(id, label, Type.SCENARIO);
        for (var step : steps) node.addChild(step);
        node.setStatus(Status.HAS_ISSUES);
        return node;
    }

    private static PlanNode ok(String label) {
        var node = new PlanNode(label, label, Type.STEP);
        node.setStatus(Status.VALIDATED);
        return node;
    }

    private static PlanNode invalid(String label, String message) {
        var node = new PlanNode(label, label, Type.STEP);
        node.setStatus(Status.INVALID);
        node.setValidationMessage(message);
        return node;
    }
}
