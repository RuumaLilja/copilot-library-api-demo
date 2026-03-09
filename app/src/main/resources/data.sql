-- Demo seed data for local/manual testing.
-- Note: book_id is generated via the book_seq sequence.

INSERT INTO books (book_id, title, author, status, version)
VALUES (NEXT VALUE FOR book_seq, 'Readable Code', 'Dustin Boswell', 'AVAILABLE', 0);

INSERT INTO books (book_id, title, author, status, version)
VALUES (NEXT VALUE FOR book_seq, 'Clean Architecture', 'Robert C. Martin', 'LOANED', 0);

INSERT INTO books (book_id, title, author, status, version)
VALUES (NEXT VALUE FOR book_seq, 'Effective Java', 'Joshua Bloch', 'AVAILABLE', 0);
