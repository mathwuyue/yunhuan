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

from basic_core import *
from fastapi import FastAPI
from model import RequestData

app = FastAPI()


@app.post("/")
async def GetResult(request: RequestData):
    if request.modelId == 2:
        return cal_basic_statistics(request)
    elif request.modelId == 3:
        return cal_test_of_normality(request)
    elif request.modelId == 6:
        return cal_test_relative(request)
