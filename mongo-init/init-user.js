db = db.getSiblingDB('compost_db');
db.createUser({
  user: "admin",
  pwd: "12345",
  roles: [{role: "readWrite", db: "compost_db"}]
});