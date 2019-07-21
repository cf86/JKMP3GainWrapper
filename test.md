
# DNAstack Interview Exercise - Solution
In this solution I give a short overview of my thinking, why some decisions were made and a short performance analysis.

First I should mention, I checked the Beacon API, but didn't anything about a reference allele. I am no biologist, and had to google the defintion:

""
When working with genome scale data the term **reference allele** refers to the base that is found in the reference genome. Since the reference is just somebody’s genome, it is not always the major allele. In contrast, the alternative allele refers to any base, other than the reference, that is found at that locus. The alternative allele is not necessarily the minor allele and it may, or may not, be linked to a phenotype. There can be more than one alternative allele per variant.
""

I am not sure, if the reference allele parameter was necessary. At least, I didn't use it. If it was necessary, then I would have needed more information about beacons and so on. But since this was just an exercice, I guess the reference allele wasn't necessary.

## Steps

1. Get all beacon IDs
2. chunk the beacon ID list to chunks of size 3
3. init completion service and submit an API call task per chunk
4. wait until a task is done and process it by checking if it timed out, is found, not found or not applicable. Furthermore, note to which organization the beacon belongs to. For this, a map organization -> counter is used to keep track.
5. create a tuple list from the organization map and afterward sort and reverse it 

## Design Decisions
In this chapter, some design decisions will be explained.

### Split list to Chunks
This would have been done in several ways, such as Google Guave, Java 8 stream API and groupBy or, as I chose, simple for loop. 
I just prefered the for loop because the performance is similiar to the other alternatives, and it is easy readable.

### Executor vs Completion Service
The parallelization process would have been done using the executor only. In this case all tasks would have been submitted and List of Futures would have been returned. So far, all good. But without the completion service the developer needs to take care if a Future is done or not. A common way is to use a while loop and until _isDone()_ returns true. The completion service' _take()_ method offers the same functionality but in a more readable and elegant way.

### Feign Timeout configuration vs executor timeout
Since Feign Client was already pre configured, it was easier to just add a read timeout to the Feign Client configuration instead of using the timeout function of the executor. Let's consider the following cases:
1. API call is successful within 30 seconds -> all good, result is returned
2. API call fails with 404, 500 or similar one -> all good, error is thrown, and task takes care about it.
3. API call takes longer than 30 seconds -> Feign Configuration throws a timeout exception and call is interrupted. Task takes care about the error handling.

In all 3 cases a result is returned from Feign Client, or a handled exception is thrown, thus, it is not necessary to implement a 2nd timeout in the executor itself.

### Sort and Reverse vs Custom Comparator
Currently, sorting and afterward reversing the list takes 2n operations, in comparion a custom Comparator would have been possible in n operations. The decision for sort and reverse is just for readability. 

### Split and Process Methods vs in-line Code
For better readability and maintainability, the logic for splitting and process the _Beacon Query result_ are outsourced to own methods. Reason is quite simple, assuming changes in the logic of the process must be changed, then only the method must be touched. There is no change necessary in the _search_ function itself. Furthermore, the _search_ function is better structured and easy to read. 

Splitting a list into chunks is more a utility function it would have been better in a Utility class. But for somplicity reasons, I left it in the BeaconSummaryController.

## Performance Analysis
This chapter is used to discuss the performance in terms of runtime and auxiliary space. 
In the following discussion, *_n_* is the number of beacon IDs fetched from the API, *_c_* the number of chunks (default 3) and *_C_* a constant value describling all constant operations independent from *_n_*.
 
### Runtime
First, let's take a look at any critical operations and the number of necessary operations:
- Line 42 - map all beacons to beacon IDs: *_n_* operations 
- Line 46 - split the list to chunks: *_n_* operations
- Line 55ff - create a task and submit to executor: *_n_* / *_c_* operations
- Line 75ff - process the beacon queries: *_n_* / *_c_* * *_c_* = *_n_* operations
- Line 92 - map organizations map to generate Organization Tuples: *_n_* operations
- line 95 - sort the list (modified merge sort): *_n * log(n)_* operations
- line 96 - reverse the list: *_n_* operations

Summarized, the performance can be described as *_f(n) = n * log(n) + 5n + C_*. So it can be stated f(n) ∈ O(*_n * log(n) + 5n + C_*) ⊆ O(*_n * log(n)_*)

### Auxiliary Space
The following operations require space depending on *_n_*:
- Line 40 - generate Beacon ID list: *_n_*
- Line 46 - split list to chunks: *_n_*
- Line 71 - organizations Map: *_n_*
- Line 90 - organizations tuple list: *_n_*
- Line 95 - modified merge Sort in O(*_n_*), simplified: *_n_*

Summarized the auxiliary space can be described as *_f(n) = 5n + C_*. So it can be stated f(n) ∈ O(*_5n + C_*) ⊆ O(*_n_*).
