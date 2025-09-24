# You will create data to be stored in your database:
# Create at least 39 weeks of sales history -- starting about one year ago and ending about today -- to store in your database that in total have approximately $750,000 in sales.
# Include 1 peak(s) days where sales are significantly higher, which typically occur at the start of the regular semester. You might also consider peak days like game days versus away games for football or relevant special holidays.
# Create inventory items for at least 16 different menu items. Remember that a given menu item will have multiple ingredients.
# You will also need other items such as cups, straws, napkins, bags, and so on.
# You are strongly encouraged to use scripting (e.g., Python) to generate `.sql` files that contain sequences of SQL commands to populate the database. You should end up with several thousand insert statements (if not tens or hundreds of thousands of them). Save all of these scripts so you can recreate anything at any time.


import random
from datetime import datetime, timedelta
import pandas as pd
import numpy as np
import os


# Set random seed for reproducibility