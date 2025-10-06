import csv
import random
from datetime import datetime, timedelta

# ----------------------------
# 1. Menu items with base prices
# (Taken from your spreadsheet image)
# ----------------------------
menu_items = [
    {"name": "Original Milk Tea", "price": 5.25},
    {"name": "Black Milk Tea", "price": 5.25},
    {"name": "Oolong Milk Tea", "price": 5.25},
    {"name": "Green Milk Tea", "price": 5.25},
    {"name": "Cappuccino Milk Tea", "price": 6.25},
    {"name": "Coconut Milk Tea", "price": 7.25},
    {"name": "Ube Milk Tea", "price": 7.25},
    {"name": "Protein Shake Milk Tea", "price": 9.75},
    {"name": "Ice Blend Latte", "price": 6.25},
    {"name": "Winter Melon Green Tea", "price": 8.25},
    {"name": "Passionfruit Green Tea", "price": 7.25},
    {"name": "Mango Green Tea", "price": 3.25},
    {"name": "Strawberry Lemonade Tea", "price": 3.25},
    {"name": "Strawberry Matcha", "price": 7.25},
    {"name": "Peach Oolong Tea", "price": 7.25},
    {"name": "Secret Matcha", "price": 69.25},
    {"name": "Free Drink", "price": 0.00},
]

# ----------------------------
# 2. Generate customer phone numbers pool
# ----------------------------
def generate_customer_pool():
    """Generate a pool of customer phone numbers that can repeat"""
    customer_phones = []
    
    # Generate 300-500 unique phone numbers for customer base
    num_customers = random.randint(300, 500)
    
    for _ in range(num_customers):
        # Generate 10-digit phone number (XXX-XXX-XXXX format without dashes)
        area_code = random.randint(200, 999)  # Valid area codes
        exchange = random.randint(200, 999)   # Valid exchange codes
        number = random.randint(1000, 9999)   # Last 4 digits
        phone = f"{area_code}{exchange}{number}"
        customer_phones.append(phone)
    
    return customer_phones

# Generate customer pool
customer_pool = generate_customer_pool()

# ----------------------------
# 3. Helper function to generate realistic order times
# ----------------------------
def generate_order_time():
    """Generate a realistic order time during business hours (9 AM - 9 PM)"""
    # Business hours: 9:00 AM to 9:00 PM (12 hours)
    # Weight distribution with natural variance and spikes
    
    # Define time periods with different probabilities
    # Morning (9-11 AM): Moderate traffic
    # Lunch rush (11 AM-2 PM): High traffic
    # Afternoon lull (2-5 PM): Lower traffic
    # Dinner rush (5-7 PM): High traffic
    # Evening (7-9 PM): Moderate traffic
    
    time_weights = {
        # Morning moderate (9-11 AM): 15% of orders
        9: 0.08, 10: 0.07,
        # Lunch rush (11 AM-2 PM): 45% of orders
        11: 0.12, 12: 0.18, 13: 0.15,
        # Afternoon lull (2-5 PM): 20% of orders
        14: 0.05, 15: 0.08, 16: 0.07,
        # Dinner rush (5-7 PM): 35% of orders
        17: 0.12, 18: 0.13,
        # Evening moderate (7-9 PM): 15% of orders
        19: 0.08, 20: 0.07
    }
    
    # Select hour based on weighted probability
    hours = list(time_weights.keys())
    weights = list(time_weights.values())
    hour = random.choices(hours, weights=weights)[0]
    
    # Add some randomness within peak periods
    if hour in [12, 13, 17, 18]:  # Peak hours
        # Slightly higher chance of orders in first half of the hour during peaks
        if random.random() < 0.6:
            minute = random.randint(0, 29)
        else:
            minute = random.randint(30, 59)
    else:
        minute = random.randint(0, 59)
    
    return hour, minute

# ----------------------------
# 4. Date range (39 weeks)
# ----------------------------
start_date = datetime.strptime("09/26/2024", "%m/%d/%Y")
num_days = 39 * 7  # 39 weeks ≈ 273 days
target_sales = 750_000
base_daily_sales = target_sales / num_days  # ≈ $2747/day

# Peak day for sales spike (semester start)
peak_day = start_date + timedelta(days=14)  # 2 weeks in
peak_multiplier = 5  # 5x spike

# ----------------------------
# 5. Generate orders
# ----------------------------
orders = []
current_date = start_date

for day_idx in range(num_days):
    date_str = current_date.strftime("%m%d%Y")
    weekday = current_date.weekday()

    # Daily sales multiplier by weekday
    multiplier = 1.0
    if weekday in [4, 5]:  # Friday/Saturday boost
        multiplier = random.uniform(1.2, 1.5)
    elif weekday == 6:  # Sunday lower
        multiplier = random.uniform(0.8, 1.0)

    # Extra spike on peak day
    if current_date.date() == peak_day.date():
        multiplier = peak_multiplier

    # Estimate number of orders for the day
    daily_target = base_daily_sales * multiplier * random.uniform(0.9, 1.1)
    avg_order_value = random.uniform(20, 35)
    num_orders = max(10, int(daily_target / avg_order_value))

    # Generate order times for the day and sort them
    order_times = [generate_order_time() for _ in range(num_orders)]
    order_times.sort()

    for order_num in range(1, num_orders + 1):
        order_items = {}
        order_total = 0.0

        # Each order has 1–4 menu items
        for _ in range(random.randint(1, 4)):
            item = random.choice(menu_items)

            # Skip free drink most of the time
            if item["name"] == "Free Drink" and random.random() > 0.05:
                continue

            small = random.randint(0, 2)
            medium = random.randint(0, 3)
            large = random.randint(0, 2)

            # Ensure at least one size is ordered
            if small == 0 and medium == 0 and large == 0:
                medium = 1

            # Price calculation
            item_total = (
                small * (item["price"] - 1)
                + medium * item["price"]
                + large * (item["price"] + 1)
            )
            order_total += item_total

            if small or medium or large:
                order_items[item["name"]] = (small, medium, large)

        # Ensure each order has at least 1 menu item
        if not order_items:
            chosen_item = random.choice(menu_items)
            order_items[chosen_item["name"]] = (0, 1, 0)
            order_total += chosen_item["price"]

        # Select a random customer phone number from the pool
        customer_id = random.choice(customer_pool)

        # Create combined date-time as 12-digit number: MMDDYYYYHHMM
        hour, minute = order_times[order_num - 1]
        datetime_combined = f"{current_date.strftime('%m%d%Y')}{hour:02d}{minute:02d}"

        orders.append({
            "DateTime": datetime_combined,
            "Order ID": f"{date_str}_{order_num}",
            "Customer ID": customer_id,
            "Menu Items": str(order_items),
            "Total Price": round(order_total, 2)
        })

    current_date += timedelta(days=1)

# ----------------------------
# 6. Write to CSV
# ----------------------------
with open("orders.csv", "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["DateTime", "Order ID", "Customer ID", "Menu Items", "Total Price"])
    writer.writeheader()
    writer.writerows(orders)

print("✅ orders.csv generated successfully!")
print(f"Total orders: {len(orders)}")
print(f"Customer pool size: {len(customer_pool)} unique phone numbers")
print("Sample orders with combined date-time:")
for i in range(min(5, len(orders))):
    print(f"  DateTime: {orders[i]['DateTime']} - Order {orders[i]['Order ID']} - Customer: {orders[i]['Customer ID']} - ${orders[i]['Total Price']}")