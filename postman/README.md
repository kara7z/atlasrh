# Simple AtlasRH Postman collection

1. Start your development database and API on port **8081**.
2. In Postman, delete the old imported collection to avoid confusion, then choose **Import → Files** and select `postman/AtlasRH.postman_collection.json`.
3. Open **AtlasRH API - Simple (port 8081)**.
4. Send **Departments → List departments** to check the connection:
   `GET http://127.0.0.1:8081/api/departments`.

There are **no variables, environments or scripts**. Every request contains a complete URL and literal example JSON. If your server uses another port, edit the URLs accordingly.

## Create test records manually

1. Create a department and note its returned `id`.
2. Create a manager: set `departmentId` in the JSON to that department ID. Note the manager's returned `id`.
3. Create an employee: set `departmentId` and `managerId` in the JSON to the actual IDs. Note the employee's returned `id`.
4. For subsequent requests, replace the example IDs in URLs and bodies with the returned IDs:
   - Department: `1`
   - Manager: `1`
   - Employee: `2`
   - Contract: `1`
   - Leave request: `1`

**These IDs are examples, not guaranteed values**, even if your database is currently empty. Employee numbers, emails and department names must be unique; edit them if you already created the examples.

## Important

- Send requests individually; this is not an automated collection-runner workflow.
- Approve, reject and cancel are **alternatives** for a pending leave. To test all three, create three separate leave requests with different, non-overlapping weekday dates and use each returned ID.
- Contract ending requires an active contract and an end date between its start and today UTC. Edit the sample dates as needed.
- Delete a department only when it has no employees. An occupied department returns 409.
- Use a **development database**: POST, PUT and DELETE requests change real records. Most created records remain because employee/contract/leave deletion endpoints do not exist.
- No authentication is configured. The manager ID in leave decisions is the application's development actor ID, not an authentication token.
