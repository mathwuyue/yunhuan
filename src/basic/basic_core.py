# Copyright 2022 Suzhou Yifei YuYue Co.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import numpy as np
import scipy as sp
import scipy.stats as stats
from model import ChartData, ResponseData


def parse_input(request):
    """Function to parse gRPC input to Python data
    - Return (xarray, yarray)
     - xarray: [[1,2,3], [2,3,4]...], for different Xs, each X is an array of numbers. Or None
     - yarray: similar to xarray or None
    """
    return (request.x, request.y)


def cal_category(xs):
    """
    keys are the categories of the variable
    """
    category = {}
    for x in xs:
        if x in category.keys():
            category[x] = category[x] + 1
        else:
            category[x] = 0
    return category


def cal_category_dist(request):
    xarrays, _ = parse_input(request)
    charts = []
    # for each xarray:
    for xs in xarrays:
        # calculate how many categories in category value
        xs_category = cal_category(xs)
        # bar chart
        # bar chart's axis is the keys of category
        # y is the number of each category
        bar_chart = ChartData(
            chartId=3,
            xaxis=[str(i) for i in xs_category.keys()],
            y=[[i + 1 for i in xs_category.values()]],
        )
        # pie chart
        # axis is the name of pie chart
        # y is value of the pie chart
        total = len(xs)
        pie_chart = ChartData(
            chartId=5,
            xaxis=[str(i) for i in xs_category.keys()],
            y=[[(i + 1) / total for i in xs_category.values()]],
        )
        # add results
        charts.append(bar_chart)
        charts.append(pie_chart)
    # compose results
    resp = ResponseData(total=2 * len(xarrays), info="ok", chartArray=charts)
    return resp


def cal_category_time_trend(request):
    pass
