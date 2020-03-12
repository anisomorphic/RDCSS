# RDCSS
This program uses a descriptor object to implement RDCSS (Restricted Double-Compare-Single-Swap).

RDCSS is defined in the following way:

word_t RDCSS(word_t *a1, word_t o1, word_t *a2, word_t o2, word_t n2)<br/>
{<br/>
  r = *a2;<br/>
  if (( r == o2) && (*a1 == o1)) *a2 = n2;<br/>
  return r;<br/>
}<br/>

RDCSS is restricted in that: a) only the location a2 can be subject to an update, b) the
memory it acts on must be partitioned into a control section (within which a1 lies) and a
data section (within which a2 lies), and c) the function returns the value from a2 rather
than an indication of success or failure.

RDCSS should operate concurrently with 1) any access to the control section, 2) reads from
the data section using a dedicated RDCSSRead operation, 3) other invocations of RDCSS,
and 4) updates to the data section from other threads that may use regular CAS, subject to
the constraint that such CAS operations may fail if an RDCSS operation is in progress. 
