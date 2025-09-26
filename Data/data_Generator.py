# You will create data to be stored in your database:
# Create at least 39 weeks of sales history -- starting about one year ago and ending about today -- to store in your database that in total have approximately $750,000 in sales.
# Include 1 peak(s) days where sales are significantly higher, which typically occur at the start of the regular semester. You might also consider peak days like game days versus away games for football or relevant special holidays.
# Create inventory items for at least 16 different menu items. Remember that a given menu item will have multiple ingredients.
# You will also need other items such as cups, straws, napkins, bags, and so on.
# You are strongly encouraged to use scripting (e.g., Python) to generate `.sql` files that contain sequences of SQL commands to populate the database. You should end up with several thousand insert statements (if not tens or hundreds of thousands of them). Save all of these scripts so you can recreate anything at any time.

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
# 2. Date range (39 weeks)
# ----------------------------
start_date = datetime.strptime("09/26/2024", "%m/%d/%Y")
num_days = 39 * 7  # 39 weeks ≈ 273 days
target_sales = 750_000
base_daily_sales = target_sales / num_days  # ≈ $2747/day

# Peak day for sales spike (semester start)
peak_day = start_date + timedelta(days=14)  # 2 weeks in
peak_multiplier = 5  # 5x spike

# ----------------------------
# 3. Generate orders
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

        orders.append({
            "Date": current_date.strftime("%m/%d/%Y"),
            "Order ID": f"{date_str}_{order_num}",
            "Menu Items": str(order_items),
            "Total Price": round(order_total, 2)
        })

    current_date += timedelta(days=1)

# ----------------------------
# 4. Write to CSV
# ----------------------------
with open("orders.csv", "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["Date", "Order ID", "Menu Items", "Total Price"])
    writer.writeheader()
    writer.writerows(orders)

print("✅ orders.csv generated successfully!")
print(f"Total orders: {len(orders)}")



