package ca.seneca.hotel.service.pricing;

/** Base decorator: wraps another {@link Billable} and adds one service's cost on top. */
public abstract class AddOnDecorator implements Billable {

    protected final Billable inner;

    protected AddOnDecorator(Billable inner) {
        this.inner = inner;
    }

    @Override
    public double getCost() {
        return inner.getCost() + addOnCost();
    }

    public abstract String getAddOnName();
    public abstract double addOnCost();
}
