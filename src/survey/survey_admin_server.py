import gevent
from gevent.pywsgi import WSGIServer
from gevent import monkey

monkey.patch_all()

from flask import Flask as _Flask, make_response, request, jsonify, send_file
from flask.globals import session

# from flask_session import Session
from flask.json import JSONEncoder as _JSONEncoder
import psycopg2
from flask_cors import *
import json
from psycopg2 import pool
from gevent.pywsgi import WSGIServer
import datetime
from decorator import login_required
from utils import *


app = _Flask(__name__, static_url_path="")
app.secret_key = (
    "67ee64d59b04c6ee69637bda9c0b7ad195bce24a309d626c1f59c6372d51f309"  # 设置session秘钥
)
app.config["SESSION_PERMANENT"] = False
app.config["SESSION_TYPE"] = "filesystem"
app.config["PERMANENT_SESSION_LIFETIME"] = datetime.timedelta(days=1)  # 设置session过期时间
app.config["SESSION_COOKIE_HTTPONLY"] = False  # 更改httponly 为 False

ps_pool = psycopg2.pool.SimpleConnectionPool(
    1, 1000, user="survey", password="survey@2021", database="survey"
)
# ps_pool = psycopg2.pool.SimpleConnectionPool(1, 1000, database="survey", user="postgres", password="123456", host="127.0.0.1", port="5432")

CORS(app, resources={r"/*": {"origins": "*"}}, supports_credentials=True)
# Session(app)


class JSONEncoder(_JSONEncoder):
    def default(self, o):
        import decimal

        if isinstance(o, decimal.Decimal):

            return float(o)

        super(JSONEncoder, self).default(o)


class Flask(_Flask):
    json_encoder = JSONEncoder


class DB:
    def __init__(self, pg_pool):
        self.pg_pool = pg_pool

    def start_db(self):
        self.ps_conn = self.pg_pool.getconn()
        while not self.ps_conn:
            gevent.sleep(1)
            ps_conn = self.pg_pool.getconn()
        cur = self.ps_conn.cursor()
        return cur

    def finish_db(self, cur):
        cur.close()
        self.ps_conn.commit()
        self.pg_pool.putconn(self.ps_conn)


class Enum:
    def __init__(self) -> None:
        self.hosipital = ""
        self.city = ""
        self.province = ""

    def hosipitalList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select id,name from hosipital")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def cityList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select id,name from city")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def provinceList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select pid,name from province")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def hosipital_name(self, num):
        """
        医院列表
        :param num:医院对应id
        :return:医院名字
        """
        for i in self.hosipital:
            if int(i["id"]) == num:
                return i["name"]
        return ""

    def hosipitalId(self, name):
        for i in self.hosipital:
            if i["name"] == name:
                return i["id"]
        return 0

    def cityId(self, name):
        for i in self.city:
            if i["name"] == name:
                return i["id"]
        return 0

    def cityName(self, id):
        for i in self.city:
            if i["id"] == id:
                return i["name"]
        return ""

    def provinceId(self, name):
        for i in self.province:
            if i["name"] == name:
                return i["pid"]
        return 0

    def provinceName(self, id):
        for i in self.province:
            if i["pid"] == id:
                return i["name"]
        return ""

    def hosipitalCount(self, id=None):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            sql = "select hosipital,num_cases from case_info group by hosipital"
            if id:
                sql = "select hosipital,count(*) from case_info group by hosipital having hosipital={}".format(
                    id
                )
            curs.execute(sql)
            list_header = [row[0] for row in curs.description]
            list_result = [[int(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
            if not res:
                res = [{"hosipital": id, "count": 0}]
            return res
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)

    def doctorLevel(self, num):
        """
        医生级别列表
        :param num:
        :return:
        """
        dict = {
            100: "专家",
            11: "普通医生",
            10: "普通医生（非会员）",
            0: "基层人员",
        }
        return dict[num]


def success(data="success"):
    res = {"status": 1, "resp": data}
    return jsonify(res), 200


def fail(status=-1, error="系统异常！请稍后重试！"):
    res = {"status": status, "resp": error}
    return jsonify(res), 404


@app.route("/hosipital", methods=["POST", "GET"])
@login_required(0)
def hosipital():
    db = DB(ps_pool)
    enum = Enum()
    enum.hosipital = enum.hosipitalList()
    # enum.city=enum.cityList()
    # enum.province=enum.provinceList()
    curs = db.start_db()
    # 添加
    if request.method == "POST":
        try:
            data = json.loads(request.get_data(as_text=True))

            sql = "insert into hosipital(name,address,level,province,city) values('{name}','{address}','{level}','{province}','{city}') RETURNING id".format(
                **data
            )
            curs.execute(sql)
            res = curs.fetchone()[0]

            # 添加管理员
            admin = data["admin"]

            if "account" not in admin.keys() or admin["account"] == "":
                admin["account"] = admin["phone"]
            if "password" not in admin.keys() or admin["password"] == "":
                admin["password"] = "12345678"
            admin["hosipital"] = res

            sql = "insert into admin(name,phone,account,password,hosipital) values('{name}','{phone}','{account}','{password}',{hosipital})".format(
                **admin
            )
            curs.execute(sql)
            return success({"id": res, "msg": "success"})
        except (psycopg2.DatabaseError, KeyError) as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 获取
    if request.method == "GET":
        data = request.args.to_dict()
        # num_page, offset
        limit, offset = handle_pagination(request)
        sql = """
            select a.id,a.name,a.address,a.level,a.num_cases,a.city,a.province
            from hosipital a, (select id from hosipital order by id desc limit {} offset {}) b where a.id = b.id and a.is_del=false
            """.format(
            limit, offset
        )
        province = data.get("province")
        if province:
            sql = """
            select a.id,a.name,a.address,a.level,a.num_cases,a.city,a.province
            from hosipital a, (select id from hosipital
            where province={} order by id desc limit {} offset {}) b where a.id=b.id and a.is_del=false
            """.format(
                province, limit, offset
            )

        city = data.get("city")
        if city:
            sql = """
            select a.id,a.name,a.address,a.level,a.num_cases,a.city,a.province
            from hosipital a, (select id from hosipital
            where city={} order by id desc limit {} offset {}) b where a.id=b.id and a.is_del=false
            """.format(
                city, limit, offset
            )
        try:
            res = curs.execute(sql)
            data = curs.fetchall()
            if not data:
                return success("")
            payload = []
            content = {}
            curs.execute("select count(*) from hosipital")
            total = curs.fetchone()[0]
            # hosipital_count=enum.hosipitalCount()
            for i in data:
                content = {
                    "id": i[0],
                    "name": i[1],
                    "address": i[2],
                    "level": i[3],
                    "city": i[5],
                    "province": i[6],
                    "num_case": i[4],
                }
                payload.append(content)
            return jsonify({"status": 1, "total": total, "resp": payload})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/hosipital/<id>", methods=["PUT", "GET", "DELETE"])
@login_required(0)
def hosipitalInfo(id):
    db = DB(ps_pool)
    enum = Enum()
    enum.hosipital = enum.hosipitalList()
    # enum.city=enum.cityList()
    # enum.province=enum.provinceList()
    curs = db.start_db()
    if request.method == "GET":
        # sql='''
        # select a.id as id,a.name as name,a.address as address,a.level as level,b.name as city,c.name as province, a.num_cases as num_cases
        # from hosipital a,city b,province c
        # where c.pid=a.province_id and b.id = a.city_id and a.id={};
        # '''.format(id)
        sql = """
            select id,name,address,level,city,province,num_cases
            from hosipital
            where id={}
            """.format(
            id
        )
        try:
            curs.execute(sql)
            data = curs.fetchall()
            if not data:
                return success("")
            i = data[0]
            curs.execute(
                "select name,phone,account from admin where hosipital={}".format(i[0])
            )
            admin_data = curs.fetchall()
            curs.execute(
                "select id,username,email,sex,phone_number,department,num_patients from doctor where hosipital=%s and is_valid=true",
                (id,),
            )
            u = curs.fetchall()
            # result construction
            payload = [
                {
                    "id": i[0],
                    "name": i[1],
                    "address": i[2],
                    "level": i[3],
                    "city": i[4],
                    "province": i[5],
                    "num_case": i[6],
                }
            ]
            if not admin_data:
                payload[0]["admin"] = {"name": "", "phone": "", "account": ""}
            else:
                payload[0]["admin"] = {
                    "name": admin_data[0][0],
                    "phone": admin_data[0][1],
                    "account": admin_data[0][2],
                }
            for i in u:
                content = {
                    "id": i[0],
                    "username": i[1],
                    "email": i[2],
                    "sex": i[3],
                    "phone_number": i[4],
                    "hosipital": enum.hosipital_name(id),
                    "hosipital_id": id,
                    "department": i[5],
                    "num_patients": i[6],
                }
                payload.append(content)
            return success(payload)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 修改
    if request.method == "PUT":
        data = json.loads(request.get_data(as_text=True))
        admin = data.get("admin")
        user = data.get("user")
        try:
            if admin:
                curs.execute(
                    "update admin set name=%s, password=%s, phone=%s where hosipital=%s",
                    (admin["name"], "12345678", admin["phone"], id),
                )
                del data["admin"]
            if user:
                curs.execute("update doctor set password='123456' where id=%s", (user,))
                del data["user"]
            if data:
                sql = (
                    "UPDATE %s SET " % "hosipital"
                    + ", ".join(
                        ["{}={}".format(str(i), repr(j)) for i, j in data.items()]
                    )
                    + " WHERE id=%s" % id
                )
                curs.execute(sql)
            # curs.fetchall()
            return success({"id": id, "msg": "success"})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)

    # 删除
    if request.method == "DELETE":
        try:
            curs.execute("select * from hosipital where id=%s", (id,))
            if not curs.fetchall()[0][0]:
                return fail(status=0, error="数据已不存在！")
            curs.execute("update hosipital set is_del=true where id=%s", (id,))
            curs.execute("delete from admin where hosipital=%s", (id,))
            curs.execute("update case_info set is_del=true where hosipital=%s", (id,))
            return success()
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/caseinfo", methods=["GET"])
@login_required(0)
def case():
    db = DB(ps_pool)
    enum = Enum()
    enum.hosipital = enum.hosipitalList()
    # enum.city=enum.cityList()
    # enum.province=enum.provinceList()
    limit, offset = handle_pagination(request)
    curs = db.start_db()
    if request.method == "GET":
        try:
            curs.execute(
                CASE_SEL_PAGE_KEYWORDS
                + """ from case_info a,doctor b where a.is_del=False and b.phone_number=a.save_user order by save_time desc limit %s offset %s""",
                (limit, offset),
            )
            all = curs.fetchall()
            payload = []
            for data in all:
                content = {
                    "id": data[47],
                    "name": data[0],
                    "phone_num1": data[1],
                    "phone_num2": data[2],
                    "sex": data[3],
                    "height": data[4],
                    "weight": data[5],
                    "id_card": data[6],
                    "province": data[7],
                    "city": data[8],
                    "district": data[9],
                    "town": data[10],
                    "street": data[11],
                    "detail_address": data[12],
                    "job": data[13],
                    "disease": data[14],
                    "health_care": data[15],
                    "surface_part": data[16],
                    "surface_area": data[17],
                    "surface_type": data[18],
                    "metabolic_ulcer": data[19],
                    "dm_time": data[20],
                    "dm_used": data[21],
                    "dm_course": data[22],
                    "df_time": data[23],
                    "df_cause": data[24],
                    "df_part": data[25],
                    "operation_part": data[26],
                    "amputation_part": data[27],
                    "smoke_drink": data[28],
                    "drink_years": data[29],
                    "drinking": data[30],
                    "start_time": data[31],
                    "before_day": data[32],
                    "cure_way": data[33],
                    "pathogenic_bacteria": data[34],
                    "hosipital": enum.hosipital_name(data[35]),
                    "hosipital_address": data[36],
                    "doctor": data[37],
                    "department": data[38],
                    "in_day": data[39],
                    "cost": data[40],
                    "course": data[41],
                    "cure_outcome": data[42],
                    "operation_course": data[43],
                    # "file":data[44],
                    "save_time": data[45],
                    "save_user": data[46],
                    "birthday": data[48],
                    "case_source": data[49],
                    "edu": data[50],
                    "dc": data[51],
                    "wagner": data[52],
                    "year": data[53],
                    "bmi": data[54],
                    "hba1c": data[55],
                    "wbc": data[56],
                    "crp": data[57],
                    "procalcitonin": data[58],
                    "fpg": data[59],
                    "uric_acid": data[60],
                    "triglyceride": data[61],
                    "total_cholesterol": data[62],
                    "hdlc": data[63],
                    "ldlc": data[64],
                    "hemoglobin": data[65],
                    "alb": data[66],
                    "abi": data[67],
                    "cre": data[68],
                    "tg": data[69],
                    "platelet": data[70],
                    "neut": data[71],
                }
                payload.append(content)
            if all:
                total = data[72]
            else:
                total = 0
            return jsonify({"status": 1, "total": total, "resp": payload})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/caseinfo/<id>", methods=["GET"])
@login_required(0)
def case_info(id):
    db = DB(ps_pool)
    enum = Enum()
    enum.hosipital = enum.hosipitalList()
    # enum.city=enum.cityList()
    # enum.province=enum.provinceList()
    curs = db.start_db()
    # 获取
    if request.method == "GET":
        sql = (
            CASE_SEL_KEYWORDS
            + """ from case_info a,doctor b where a.id={} and a.is_del=False and b.phone_number=a.save_user order by desc;""".format(
                id
            )
        )
        try:
            res = curs.execute(sql)
            all = curs.fetchall()
            if not all:
                return success("")
            data = all[0]
            payload = {
                "id": data[47],
                "name": data[0],
                "phone_num1": data[1],
                "phone_num2": data[2],
                "sex": data[3],
                "height": data[4],
                "weight": data[5],
                "id_card": data[6],
                "province": data[7],
                "city": data[8],
                "district": data[9],
                "town": data[10],
                "street": data[11],
                "detail_address": data[12],
                "job": data[13],
                "disease": data[14],
                "health_care": data[15],
                "surface_part": data[16],
                "surface_area": data[17],
                "surface_type": data[18],
                "metabolic_ulcer": data[19],
                "dm_time": data[20],
                "dm_used": data[21],
                "dm_course": data[22],
                "df_time": data[23],
                "df_cause": data[24],
                "df_part": data[25],
                "operation_part": data[26],
                "amputation_part": data[27],
                "smoke_drink": data[28],
                "drink_years": data[29],
                "drinking": data[30],
                "start_time": data[31],
                "before_day": data[32],
                "cure_way": data[33],
                "pathogenic_bacteria": data[34],
                "hosipital": enum.hosipital_name(data[35]),
                "hosipital_address": data[36],
                "doctor": data[37],
                "department": data[38],
                "in_day": data[39],
                "cost": data[40],
                "course": data[41],
                "cure_outcome": data[42],
                "operation_course": data[43],
                # "file":data[44],
                "save_time": data[45],
                "save_user": data[46],
                "birthday": data[48],
                "case_source": data[49],
                "edu": data[50],
                "dc": data[51],
                "wagner": data[52],
                "year": data[53],
                "bmi": data[54],
                "hba1c": data[55],
                "wbc": data[56],
                "crp": data[57],
                "procalcitonin": data[58],
                "fpg": data[59],
                "uric_acid": data[60],
                "triglyceride": data[61],
                "total_cholesterol": data[62],
                "hdlc": data[63],
                "ldlc": data[64],
                "hemoglobin": data[65],
                "alb": data[66],
                "abi": data[67],
                "cre": data[68],
                "tg": data[69],
                "platelet": data[70],
                "neut": data[71],
            }

            return success(payload)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/login", methods=["POST"])
def admin_in():
    if request.method == "POST":
        db = DB(ps_pool)
        enum = Enum()
        curs = db.start_db()
        data = json.loads(request.get_data(as_text=True))
        if data.get("username") != "admin":
            return fail(status=0, error="用户名或密码错误！")
        sql = "select id from admin where phone='{username}' and password='{password}'".format(
            **data
        )
        try:
            curs.execute(sql)
            res = curs.fetchall()
            if not res:
                return fail(status=0, error="用户名或密码错误！")

            session["username"] = data["username"]
            session["role"] = 999

            return success("登录成功")
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


# 注销用户
@app.route("/logout", methods=["GET"])
def logout():
    session.clear()
    return jsonify(msg="退出成功")


@app.route("/session", methods=["GET"])
def ret():
    name = session.get("username")
    role = session.get("role")
    return success({"name": name, "role": role})


@app.route("/export", methods=["GET"])
@login_required(999)
def export_all():
    db = DB(ps_pool)
    curs = db.start_db()
    try:
        with open("/home/skinphotodev/zjcmzx/export.csv", "w") as wf:
            curs.copy_to(wf, "case_info", sep=",")
        return send_file(
            "/home/skinphotodev/zjcmzx/export.csv",
            mimetype="text/csv",
            attachment_filename="export.csv",
            as_attachment=True,
        )
    except psycopg2.DatabaseError as error:
        return jsonify({"status": -1, "resp": str(error)})
    finally:
        db.finish_db(curs)


if __name__ == "__main__":
    # app.run()
    http_server = WSGIServer(("127.0.0.1", 6001), app)
    http_server.serve_forever()
